package br.dev.order.schedule

import br.dev.order.domain.Order
import br.dev.order.entity.OrderEntity
import br.dev.order.entity.OrderItemEntity
import br.dev.order.entity.OrderItemType
import br.dev.order.entity.OrderStatus
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Component
class CancelOrdersSchedule {

    private val logger = KotlinLogging.logger {  }

    @Transactional
    @Scheduled(cron = "0 */3 * * * *")
    fun transitionToCancel() {
        logger.info { "Transition Orders to CANCELED" }

        val orders = OrderEntity.selectAll()
            .where { (OrderEntity.amount greater BigDecimal(50.00)) and (OrderEntity.status eq OrderStatus.COMPLETED) }
            .toList()
            .map(::toOrder)

        val idsToUpdate = orders.map { it.id!! }

        OrderEntity.update({ OrderEntity.id inList idsToUpdate }) {
            it[status] = OrderStatus.CANCELED
            it[updatedAt] = LocalDateTime.now()
        }

        orders.map(::createCancelItem)
    }

    /*
        10% de multa
     */
    fun createCancelItem(order: Order): EntityID<Int> = OrderItemEntity.insertAndGetId {
        it[orderItemType] = OrderItemType.CANCELATION_FEE
        it[orderExternalId] = order.externalId
        it[externalId] = UUID.randomUUID().toString()
        it[createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
        it[amount] = order.amount * BigDecimal(0.1)
    }

    fun toOrder(result: ResultRow): Order = Order(
        id = result[OrderEntity.id].value,
        status = result[OrderEntity.status],
        externalId = result[OrderEntity.externalId],
        amount = result[OrderEntity.amount]
    )
}