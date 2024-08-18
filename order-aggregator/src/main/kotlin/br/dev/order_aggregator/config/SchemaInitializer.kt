package br.dev.order_aggregator.config

import br.dev.order_aggregator.handler.EventEntity
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class SchemaInitializer : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments?) {
        SchemaUtils.createMissingTablesAndColumns(tables = arrayOf(EventEntity))
    }
}