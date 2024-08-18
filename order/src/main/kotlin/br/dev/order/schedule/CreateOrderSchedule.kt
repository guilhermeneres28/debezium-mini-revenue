package br.dev.order.schedule

import br.dev.order.domain.Order
import br.dev.order.domain.OrderItem
import br.dev.order.entity.OrderEntity
import br.dev.order.entity.OrderItemEntity
import br.dev.order.entity.OrderItemType
import br.dev.order.entity.OrderStatus
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/*
    Schedule para gerar Orders com status PENDING a cada 1 minuto
    TODO: Talvez colocar as operações com o banco de dados em outra camada seja uma boa, por enquanto não achei complexo
 */
@Component
class CreateOrderSchedule {

    private val logger = KotlinLogging.logger {  }

    @Transactional
    @Scheduled(cron = "0 */1 * * * *")
    fun generateOrders() {

        logger.info { "Generating Order" }

        val randomAmount =  ThreadLocalRandom.current().nextDouble(10.0, 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_DOWN)

        val ticket = OrderItem(
            type = OrderItemType.TICKET,
            externalId = UUID.randomUUID().toString(),
            amount = randomAmount
        )

        /*
            20% de serviceFee
         */
        val serviceFee = OrderItem(
            type = OrderItemType.SERVICE_FEE,
            externalId = UUID.randomUUID().toString(),
            amount = ticket.amount.multiply(BigDecimal(0.2)).setScale(2, RoundingMode.HALF_DOWN)
        )

        val order = Order(
            status = OrderStatus.CREATED,
            externalId = UUID.randomUUID().toString(),
            items = setOf(ticket, serviceFee),
            amount = (ticket.amount + serviceFee.amount).setScale(2, RoundingMode.HALF_DOWN)
        )

        val orderId = OrderEntity.insertAndGetId {
            it[status] = order.status
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
            it[externalId] = order.externalId
            it[amount] = order.amount.setScale(2, RoundingMode.HALF_DOWN)
        }

        OrderItemEntity.insertAndGetId {
            it[orderItemType] = ticket.type
            it[orderExternalId] = order.externalId
            it[externalId] = ticket.externalId
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
            it[amount] = ticket.amount.setScale(2, RoundingMode.HALF_DOWN)
        }

        OrderItemEntity.insertAndGetId {
            it[orderItemType] = serviceFee.type
            it[orderExternalId] = externalId
            it[externalId] = serviceFee.externalId
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
            it[amount] = serviceFee.amount.setScale(2, RoundingMode.HALF_DOWN)
        }
    }
}