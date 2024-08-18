package br.dev.order_aggregator.handler

import br.dev.order_aggregator.emit.Event
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json

enum class ProcessingStatus{CREATED, PUB_ERROR, SUCCESS}
enum class TopicName{ORDER, ORDER_ITEM}

val mapper = jacksonObjectMapper()

object EventEntity : IntIdTable("event") {
    val eventBody = json("event_body", { mapper.writeValueAsString(it)}, { mapper.readValue<Map<String, Any>>(it)})
    val createdAt = datetime("created_at")
    val topic =  enumerationByName("topicName", 10, TopicName::class)
    val processingStatus = enumerationByName("processing_status", 10, ProcessingStatus::class)
}