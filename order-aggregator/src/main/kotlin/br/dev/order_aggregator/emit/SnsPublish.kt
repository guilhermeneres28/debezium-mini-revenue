package br.dev.order_aggregator.emit

import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.PublishRequest
import aws.smithy.kotlin.runtime.net.url.Url
import br.dev.order_aggregator.handler.EventEntity
import br.dev.order_aggregator.handler.ProcessingStatus
import br.dev.order_aggregator.handler.TopicName
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class Event(
    val id: Int,
    val body: Map<String, Any>, val topicName: TopicName,
    val processingStatus: ProcessingStatus
)

const val orderTopicArn = "arn:aws:sns:eu-central-1:000000000000:order-debezium-events"
const val orderItemTopicArn = "arn:aws:sns:eu-central-1:000000000000:order-item-debezium-events"

@Component
class SnsPublish {

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    fun process() {
        collectCreatedEventsFromDatabase().forEach {
            when (it.topicName) {
                TopicName.ORDER -> publish(orderTopicArn, it)
                TopicName.ORDER_ITEM -> publish(orderItemTopicArn, it)
            }
        }
    }

    fun publish(topicArnValue: String, event: Event) {
        val req = PublishRequest {
            message = event.body.toString()
            topicArn = topicArnValue
        }

        runBlocking {
            val client = SnsClient.fromEnvironment {
                credentialsProvider = ProfileCredentialsProvider(profileName = "test-profile")
                endpointUrl = Url.parse("http://127.0.0.1:4566")
                region = "eu-central-1"
            }
            logger.info { "Trying to publish sns event: ${event.body}" }
            val result = client.publish(req)
            if (result.messageId != null) updateEventStatus(event, ProcessingStatus.SUCCESS) else updateEventStatus(
                event,
                ProcessingStatus.PUB_ERROR
            )
        }

    }

    fun updateEventStatus(event: Event, status: ProcessingStatus) {
        EventEntity.update({ EventEntity.id eq event.id }) {
            it[processingStatus] = status
        }
    }

    fun collectCreatedEventsFromDatabase() = EventEntity.selectAll()
        .where { EventEntity.processingStatus eq ProcessingStatus.CREATED }
        .toList()
        .map(::toEvent)

    fun toEvent(resultRow: ResultRow): Event = Event(
        id = resultRow[EventEntity.id].value,
        body = resultRow[EventEntity.eventBody],
        topicName = resultRow[EventEntity.topic],
        processingStatus = resultRow[EventEntity.processingStatus]
    )

    val logger = KotlinLogging.logger { }
}