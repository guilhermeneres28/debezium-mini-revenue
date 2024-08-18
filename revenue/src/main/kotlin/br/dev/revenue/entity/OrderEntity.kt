package br.dev.revenue.entity

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object OrderEntity: IntIdTable("orders") {
    val orderStatus = varchar("order_status")
    val createdAt = datetime("created_date")
    val externalId = varchar("string")
}