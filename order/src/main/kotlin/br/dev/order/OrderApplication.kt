package br.dev.order

import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@EnableScheduling
@ImportAutoConfiguration(ExposedAutoConfiguration::class)
@SpringBootApplication
class OrderApplication

fun main(args: Array<String>) {
	runApplication<OrderApplication>(*args)
}

enum class OrderStatus {CREATED, PENDING, COMPLETED, CANCELED}

object OrderEntity : IntIdTable() {
	val status = enumerationByName("order_status", 10, OrderStatus::class)
	val createdAt = datetime("created_at")
	val updatedAt = datetime("updated_at")
	val externalId = uuid("external_id")
	val amount = decimal("amount", 4, 2)
}

enum class OrderItemType {TICKET, SERVICE_FEE, CANCELATION_FEE}

object OrderItemEntity: IntIdTable() {
	val orderItemType = enumerationByName("order_item_type", 20,  OrderItemType::class)
	val orderTId = reference("order_id", OrderEntity.id, onDelete = ReferenceOption.CASCADE)
	val externalId = uuid("external_id")
	val createdAt = datetime("created_at")
	val updatedAt = datetime("updated_at")
	val amount = decimal("amount", 4, 2)
}

data class Order(
	val id: Int? = null,
	val status: OrderStatus,
	val externalId: UUID,
	val items: Set<OrderItem>? = null,
	val amount: BigDecimal
)

data class OrderItem(
	val type: OrderItemType,
	val externalId: UUID,
	val amount: BigDecimal
)

@Component
class SchemaInitialize : ApplicationRunner {

	@Transactional
	override fun run(args: ApplicationArguments?) {
		SchemaUtils.createMissingTablesAndColumns(tables = arrayOf(OrderEntity, OrderItemEntity) )
	}
}

@Component
class CreateOrderSchedule {

	private val logger = KotlinLogging.logger {  }

	@Transactional
	@Scheduled(fixedDelay = 20000)
	fun generateOrders() {

		logger.info { "Generating Order" }

		val randomAmount =  ThreadLocalRandom.current().nextDouble(10.0, 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_DOWN)

		val ticket = OrderItem(
			type = OrderItemType.TICKET,
			externalId = UUID.randomUUID(),
			amount = randomAmount
		)

		/*
    		20% de serviceFee
 		*/
		val serviceFee = OrderItem(
			type = OrderItemType.SERVICE_FEE,
			externalId = UUID.randomUUID(),
			amount = ticket.amount.multiply(BigDecimal(0.2)).setScale(2, RoundingMode.HALF_DOWN)
		)

		val order = Order(
			status = OrderStatus.CREATED,
			externalId = UUID.randomUUID(),
			items = setOf(ticket, serviceFee),
			amount = (ticket.amount + serviceFee.amount).setScale(2, RoundingMode.HALF_DOWN)
		)

		val orderId = OrderEntity.insertAndGetId {
			it[status] = order.status
			it[createdAt] = LocalDateTime.now()
			it[updatedAt] = LocalDateTime.now()
			it[externalId] = UUID.randomUUID()
			it[amount] = order.amount.setScale(2, RoundingMode.HALF_DOWN)
		}

		OrderItemEntity.insertAndGetId {
			it[orderItemType] = ticket.type
			it[orderTId] = orderId
			it[externalId] = ticket.externalId
			it[createdAt] = LocalDateTime.now()
			it[updatedAt] = LocalDateTime.now()
			it[amount] = ticket.amount.setScale(2, RoundingMode.HALF_DOWN)
		}

		OrderItemEntity.insertAndGetId {
			it[orderItemType] = serviceFee.type
			it[orderTId] = orderId
			it[externalId] = serviceFee.externalId
			it[createdAt] = LocalDateTime.now()
			it[updatedAt] = LocalDateTime.now()
			it[amount] = serviceFee.amount.setScale(2, RoundingMode.HALF_DOWN)
 		}
	}
}

@Component
class TransitionOrderToPendingSchedule {

	private val logger = KotlinLogging.logger {  }

	@Transactional
	@Scheduled(fixedDelay = 50000)
	fun transitionToPending() {

		logger.info { "Transition Orders to PENDING" }

		OrderEntity.update({ OrderEntity.status eq OrderStatus.CREATED }) {
			it[status] = OrderStatus.PENDING
			it[updatedAt] = LocalDateTime.now()
		}
	}
}

@Component
class TransitionOrderToCompleteSchedule {

	private val logger = KotlinLogging.logger {  }

	@Transactional
	@Scheduled(fixedDelay = 60000)
	fun transitionToPending() {

		logger.info { "Transition Orders to COMPLETED" }

		OrderEntity.update({ OrderEntity.status eq OrderStatus.PENDING }) {
			it[status] = OrderStatus.COMPLETED
			it[updatedAt] = LocalDateTime.now()
		}
	}
}

@Component
class CancelSomeOrdersSchedule {

	private val logger = KotlinLogging.logger {  }

	@Transactional
	@Scheduled(fixedDelay = 80000)
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
		it[orderTId] = order.id ?: 0
		it[externalId] = UUID.randomUUID()
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

