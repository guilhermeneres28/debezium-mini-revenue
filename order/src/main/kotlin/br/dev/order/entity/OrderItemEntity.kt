package br.dev.order.entity

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

enum class OrderItemType { TICKET, SERVICE_FEE, CANCELATION_FEE }

object OrderItemEntity : IntIdTable("orders_item") {
    val orderItemType = enumerationByName("order_item_type", 20, OrderItemType::class)
    val orderExternalId = reference("order_external_id", OrderEntity.externalId, onDelete = ReferenceOption.CASCADE)
    val externalId = varchar("external_id")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val amount = decimal("amount", 4, 2)
}