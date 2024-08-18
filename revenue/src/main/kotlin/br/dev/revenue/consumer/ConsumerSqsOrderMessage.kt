package br.dev.revenue.consumer

import br.dev.revenue.OrderEvent
import br.dev.revenue.entity.OrderEntity
import br.dev.revenue.infrastructure.SqsClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.insertIgnore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

val orderQueueUrl = "http://sqs.eu-central-1.localhost.localstack.cloud:4566/000000000000/order-sqs-queue"

/*
    TODO: Transformar em um ApplicationRunner
 */
@Component
class ConsumerSqsOrderMessage(val sqsClient: SqsClient) {

    val mapper = ObjectMapper()

    @Transactional
    suspend fun processOrder() {
        while(true) {
            val messages = sqsClient.consumeSQSMessage(orderQueueUrl).messages

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