package br.dev.revenue.domain

import br.dev.revenue.entity.Operation
import java.math.BigDecimal
import java.time.LocalDateTime

data class Revenue(
    val operation: Operation,
    val revenueDate: LocalDateTime,
    val type: String,
    val orderExternalId: String,
    val amount: BigDecimal
)
