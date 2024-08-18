package br.dev.revenue.consumer

import br.dev.revenue.OrderEvent
import br.dev.revenue.OrderItemEvent
import br.dev.revenue.domain.Revenue
import br.dev.revenue.entity.Operation
import br.dev.revenue.entity.OrderEntity
import br.dev.revenue.entity.RevenueEntity
import br.dev.revenue.infrastructure.SqsClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

val orderItemQueueUrl = "http://sqs.eu-central-1.localhost.localstack.cloud:4566/000000000000/order-item-sqs-queue"

/*
    TODO: Transformar em um ApplicationRunner
    TODO: Falta excluir mensagens que foram processadas do SQS
 */
@Component
class ConsumerSqsOrderItemMessage(val sqsClient: SqsClient) {
    val mapper = ObjectMapper()
    suspend fun processOrderItem() {
        while (true) {
            val messages = sqsClient.consumeSQSMessage(orderItemQueueUrl).messages
            if (messages != null) {
                val (ordersItensToApplyRule, ordersItemNotAvailable) = messages.map {
                    mapper.readValue(
                        it.body,
                        OrderItemEvent::class.java
                    )
                }.filter { it.orderItemType.equals("SERVICE_FEE") or it.orderItemType.equals("CANCELATION_FEE") }
                    .toList()
                    .partition { shouldApplyRevenueRule(it) }

                applyRevenueRule(ordersItensToApplyRule)
            }
        }
    }

    /*
        TODO: Implementar um count
        TODO: Implementar para buscar todas as orders de uma vez no banco para evitar roundtrip
    */
    fun shouldApplyRevenueRule(orderItemEvent: OrderItemEvent): Boolean {
        val order = OrderEntity.selectAll()
            .where { OrderEntity.externalId eq orderItemEvent.orderExternalId }
            .firstOrNull()

        return order != null
    }

    suspend fun applyRevenueRule(orderItens: List<OrderItemEvent>): Result<Unit> = runCatching {
        val revenues = orderItens.map { createRevenue(it) }
            .toList()

        RevenueEntity.batchInsert(revenues) { (operation, revenueDate, type, orderExternalId, amount) ->
            this[RevenueEntity.operation] = operation
            this[RevenueEntity.revenueDate] = revenueDate
            this[RevenueEntity.type] = type
            this[RevenueEntity.orderExternalId] = orderExternalId
            this[RevenueEntity.amount] = amount
        }
    }

    suspend fun createRevenue(orderItemEvent: OrderItemEvent) = Revenue(
        operation = findOperationTypeByOrderStatus(orderItemEvent.orderExternalId),
        revenueDate = orderItemEvent.createdAt,
        type = orderItemEvent.orderItemType,
        orderExternalId = orderItemEvent.orderExternalId,
        amount = orderItemEvent.amount
    )

    suspend fun findOperationTypeByOrderStatus(orderExternalId: String): Operation {
        val order = OrderEntity.selectAll()
            .where { OrderEntity.externalId eq orderExternalId }
            .map { toOrderEvent(it) }
            .firstOrNull()

        return when (order!!.orderStatus) {
            "COMPLETED" -> Operation.INCREASE
            "CANCELED" -> Operation.DECREASE
            else -> Operation.INCREASE
        }
    }

    fun toOrderEvent(resultRow: ResultRow) = OrderEvent(
        orderStatus = resultRow[OrderEntity.orderStatus],
        createdAt = resultRow[OrderEntity.createdAt],
        externalId = resultRow[OrderEntity.externalId]
    )
}