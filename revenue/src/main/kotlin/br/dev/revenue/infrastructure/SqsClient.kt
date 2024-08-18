package br.dev.revenue.infrastructure

import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import aws.smithy.kotlin.runtime.net.url.Url
import org.springframework.stereotype.Component

@Component
class SqsClient {
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
}