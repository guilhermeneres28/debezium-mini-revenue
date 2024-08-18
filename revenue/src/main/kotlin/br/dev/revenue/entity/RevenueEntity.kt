package br.dev.revenue.entity

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

enum class Operation{INCREASE, DECREASE}

object RevenueEntity: IntIdTable("revenues") {
    val revenueDate = datetime("created_date")
    val type = varchar("type")
    val operation = enumerationByName("operation", 10, Operation::class)
    val amount = decimal("amount", 4, 2 )
    val orderExternalId = varchar("order_external_id")
}