package br.dev.order.entity

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

enum class OrderStatus {CREATED, PENDING, COMPLETED, CANCELED}

object OrderEntity : IntIdTable("orders") {
    val status = enumerationByName("order_status", 10, OrderStatus::class)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val externalId = varchar("external_id")
    val amount = decimal("amount", 4, 2)
}