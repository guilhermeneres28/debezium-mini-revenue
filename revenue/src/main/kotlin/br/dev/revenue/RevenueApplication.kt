package br.dev.revenue

import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import aws.smithy.kotlin.runtime.net.url.Url
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@ImportAutoConfiguration(ExposedAutoConfiguration::class)
@SpringBootApplication
class RevenueApplication

fun main(args: Array<String>) {
	runApplication<RevenueApplication>(*args)
}

val orderItemQueueUrl = "http://sqs.eu-central-1.localhost.localstack.cloud:4566/000000000000/order-item-sqs-queue"
val orderQueueUrl = "http://sqs.eu-central-1.localhost.localstack.cloud:4566/000000000000/order-sqs-queue"

/*
	{"id":24,"order_status":"CREATED","created_at":1723911324611247,"updated_at":1723911324611255,"external_id":"w/Z7GvMEQpWxYBinvwa4EA==","amount":58.22}
 */
data class OrderEvent(val orderStatus: String, val createdAt: LocalDateTime, val externalId: String)

object OrderEntity: IntIdTable("orders") {
	val orderStatus = varchar("order_status")
	val createdAt = datetime("created_date")
	val externalId = varchar("string")
}

enum class Operation{INCREASE, DECREASE}

object RevenueEntity: IntIdTable("revenues") {
	val revenueDate = datetime("created_date")
	val type = varchar("type")
	val operation = enumerationByName("operation", 10, Operation::class)
	val amount = decimal("amount", 4, 2 )
	val orderExternalId = varchar("order_external_id")
}
/*s
	{"id":55,"order_item_type":"TICKET","order_id":24,"external_id":"+pspB3NQQPCqNYFHLrYZVw==","created_at":1723911324620536,"updated_at":1723911324620543,"amount":48.52}
 */
data class OrderItemEvent(val orderItemType: String, val orderExternalId: String, val createdAt: LocalDateTime, val amount: BigDecimal)

@Component
class ConsumerSqsOrderMessage {
	val mapper = ObjectMapper()

	@Transactional
	suspend fun processOrder() {
		while(true) {
			val messages = consumeSQSMessage(orderQueueUrl).messages

			if(messages != null) {
				messages.map { mapper.readValue(it.body, OrderEvent::class.java) }
					.toList()
					.map { saveOrder(it) }
			}
		}
	}

	suspend fun saveOrder(event: OrderEvent) {
		OrderEntity.insertIgnore {
			it[orderStatus] = event.orderStatus
			it[createdAt] = event.createdAt
			it[externalId] = event.externalId
		}
	}
}


data class Revenue(val operation: Operation, val revenueDate: LocalDateTime, val type: String, val orderExternalId: String, val amount: BigDecimal)

@Component
class ConsumerSqsOrderItemMessage {
	val mapper = ObjectMapper()
	suspend fun processOrderItem() {
		while (true) {
			val messages = consumeSQSMessage(orderQueueUrl).messages
			if(messages != null) {
				val (ordersItensToApplyRule, ordersItemNotAvailable) = messages.map { mapper.readValue(it.body, OrderItemEvent::class.java) }
					.filter { it.orderItemType.equals("SERVICE_FEE") or it.orderItemType.equals("CANCELATION_FEE") }
					.toList()
					.partition { shouldApplyRevenueRule(it) }

				applyRevenueRule(ordersItensToApplyRule)
			}
		}
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

	suspend fun createRevenue(orderItemEvent: OrderItemEvent) = Revenue(
		operation = findOperationTypeByOrderStatus(orderItemEvent.orderExternalId),
		revenueDate =  orderItemEvent.createdAt,
		type = orderItemEvent.orderItemType,
		orderExternalId = orderItemEvent.orderExternalId,
		amount = orderItemEvent.amount
	)

	/*
		TODO: Implementar um count
	 */
	fun shouldApplyRevenueRule(orderItemEvent: OrderItemEvent): Boolean {
		val order = OrderEntity.selectAll()
			.where { OrderEntity.externalId eq orderItemEvent.orderExternalId }
			.firstOrNull()

		return order != null
	}

	fun toOrderEvent(resultRow: ResultRow) = OrderEvent(
		orderStatus = resultRow[OrderEntity.orderStatus],
		createdAt = resultRow[OrderEntity.createdAt],
		externalId = resultRow[OrderEntity.externalId]
	)
}

suspend fun consumeSQSMessage(queueUrlArn: String): ReceiveMessageResponse {
	val receiveMessageRequest = ReceiveMessageRequest {
		queueUrl = queueUrlArn
		maxNumberOfMessages = 10
	}

	val client = SqsClient.fromEnvironment {
		credentialsProvider = ProfileCredentialsProvider(profileName = "test-profile")
		endpointUrl = Url.parse("http://127.0.0.1:4566")
		region = "eu-central-1"
	}

	// Tratar possivel falha
	val response = client.receiveMessage(receiveMessageRequest)
	return response
}
