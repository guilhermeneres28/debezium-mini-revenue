package br.dev.order.domain

import br.dev.order.entity.OrderStatus
import java.math.BigDecimal

data class Order(
    val id: Int? = null,
    val status: OrderStatus,
    val externalId: String,
    val items: Set<OrderItem>? = null,
    val amount: BigDecimal
)