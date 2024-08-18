package br.dev.revenue

import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import aws.smithy.kotlin.runtime.net.url.Url
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.date
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
data class OrderEvent(val orderStatus: String, val createdAt: LocalDateTime, val externalId: String, val amount: BigDecimal)

object OrdersEntity: IntIdTable("orders") {
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
					.map {::saveOrder}
			}

		}
	}

	suspend fun saveOrder(event: OrderEvent) {
		OrdersEntity.insertIgnore {
			it[orderStatus] = event.orderStatus
			it[createdAt] = event.createdAt
			it[externalId] = event.externalId
		}
	}
}


data class Revenue(val operation: Operation, val)

@Component
class ConsumerSqsOrderItemMessage {
	val mapper = ObjectMapper()
	suspend fun processOrderItem() {
		while (true) {
			val messages = consumeSQSMessage(orderQueueUrl).messages
			if(messages != null) {
				messages.map { mapper.readValue(it.body, OrderItemEvent::class.java) }
					.toList()
					.map {::applyRevenueRule }
					.map {::saveRevenue}
			}
		}
	}

	/*
		Devido a falta de implementação do outbox sera necessario fazer conciliação de orders
	 */
	suspend fun applyRevenueRule(orderItemEvent: OrderItemEvent): List<Revenue> {
		OrdersEntity.select()
			.where {}
	}

	suspend fun saveRevenue(event:RevenueEntity) {

	}
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