package br.dev.order_aggregator.handler

import br.dev.order_aggregator.emit.SnsPublish
import br.dev.order_aggregator.handler.Operation.*
import io.debezium.config.Configuration
import io.debezium.embedded.Connect
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.RecordChangeEvent
import io.debezium.engine.format.ChangeEventFormat
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.apache.kafka.connect.data.Field
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

enum class Operation(val value: String){
    READ("r"),
    DELETE("d"),
    CREATE("c"),
    UPDATE("u")
}

@Component
class DebeziumEventHandler(
    orderConnector: Configuration,
    private val snsPublish: SnsPublish,
) {
    lateinit var debeziumEngine: DebeziumEngine<RecordChangeEvent<SourceRecord>>

    init {
        this.debeziumEngine = DebeziumEngine.create(ChangeEventFormat.of(Connect::class.java))
            .using(orderConnector.asProperties())
            .notifying(this::handlerEvent)
            .build()
    }

    fun handlerEvent(sourceRecord: RecordChangeEvent<SourceRecord>) {
        val sourceRecordValue  = sourceRecord.record().value() as Struct
        val operation = sourceRecordValue.get(io.debezium.data.Envelope.FieldName.OPERATION)

        val queryDebeziumPayloadBasedOnOperation = when(operation) {
            CREATE.value, UPDATE.value -> io.debezium.data.Envelope.FieldName.AFTER
            DELETE.value-> io.debezium.data.Envelope.FieldName.BEFORE
            else -> "" // n sei se é a melhor alternativa
        }

        if(operation != READ.value) {
            val debeziumPayloadSelected = sourceRecordValue.get(queryDebeziumPayloadBasedOnOperation) as Struct
            val event = debeziumPayloadSelected.schema().fields().map(Field::name)
                .associateWith { fieldName -> debeziumPayloadSelected.get(fieldName) }

            val tableName = (sourceRecordValue.get(io.debezium.data.Envelope.FieldName.SOURCE) as Struct).get("table")
            when(tableName) {
                "OrderEntity" -> sendEventToOrderTopic(event)
                "OrderItemEntity" -> sendEventToOrderItemTopic(event)
            }
        }
    }

    fun sendEventToOrderTopic(event: Map<String, Any>) {
        val topicArn = "arn:aws:sns:eu-central-1:000000000000:order-debezium-events"
        snsPublish.publish(topicArn, event.toString())
    }

    fun sendEventToOrderItemTopic(event: Map<String, Any>) {
        val topicArn = "arn:aws:sns:eu-central-1:000000000000:order-item-debezium-events"
        snsPublish.publish(topicArn, event.toString())
    }

    val executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    fun start() = executor.execute(debeziumEngine)

    @PreDestroy
    fun stop() {
        this.debeziumEngine.close()
    }

    val logger = KotlinLogging.logger {  }
}