package br.dev.revenue.config

import br.dev.revenue.Orders
import br.dev.revenue.Revenue
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SchemaInitializer : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        SchemaUtils.createMissingTablesAndColumns(tables = arrayOf(Orders, Revenue))
    }
}