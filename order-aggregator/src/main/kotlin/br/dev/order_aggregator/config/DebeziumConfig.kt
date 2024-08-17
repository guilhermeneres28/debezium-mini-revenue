package br.dev.order_aggregator.config

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class DebeziumConfig {
    @Bean
    fun orderConnector(): io.debezium.config.Configuration =
        io.debezium.config.Configuration.create()
            .with("name", "order-mysql-connector")
            .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
            .with("offset.storage.file.filename", "/tmp/offsets.dat")
            .with("offset.flush.interval.ms", "60000")
            .with("database.hostname", "localhost")
            .with("database.port", "3306")
            .with("database.user", "root")
            .with("database.password", "admin")
            .with("database.dbname", "orders")
            .with("database.include.list", "orders")
            .with("database.server.id", "90001")
            .with("topic.prefix", "my-app-connector")
            .with("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory")
            .with("schema.history.internal.file.filename", "/tmp/schemahistory.dat")
            .with("key.converter.schemas.enable", "false")
            .with("value.converter.schemas.enable", "false")
            .build()
}