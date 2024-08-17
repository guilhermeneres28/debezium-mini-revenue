package br.dev.order_aggregator.emit

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.PublishRequest
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class SnsPublish {

    fun publish(topicArnValue: String, event: String) {
        val req = PublishRequest {
            message = event
            topicArn = topicArnValue
        }

        SnsClient {region = "eu-central-1"}.use { snsClient ->
                val result = snsClient.publish(req)
                logger.info { "${result.messageId} message sent to sns" }
        }
    }

    val logger = KotlinLogging.logger {  }
}