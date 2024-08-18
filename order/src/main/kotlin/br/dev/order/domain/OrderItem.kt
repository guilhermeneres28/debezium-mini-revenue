package br.dev.order.domain

import br.dev.order.entity.OrderItemType
import java.math.BigDecimal

data class OrderItem(
    val type: OrderItemType,
    val externalId: String,
    val amount: BigDecimal
)