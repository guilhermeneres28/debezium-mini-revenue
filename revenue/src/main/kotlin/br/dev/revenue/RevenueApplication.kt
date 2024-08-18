package br.dev.revenue

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.math.BigDecimal
import java.time.LocalDateTime

@ImportAutoConfiguration(ExposedAutoConfiguration::class)
@SpringBootApplication
class RevenueApplication

fun main(args: Array<String>) {
	runApplication<RevenueApplication>(*args)
}




/*
	{"id":24,"order_status":"CREATED","created_at":1723911324611247,"updated_at":1723911324611255,"external_id":"w/Z7GvMEQpWxYBinvwa4EA==","amount":58.22}
 */
data class OrderEvent(val orderStatus: String, val createdAt: LocalDateTime, val externalId: String)

/*s
	{"id":55,"order_item_type":"TICKET","order_id":24,"external_id":"+pspB3NQQPCqNYFHLrYZVw==","created_at":1723911324620536,"updated_at":1723911324620543,"amount":48.52}
 */
data class OrderItemEvent(val orderItemType: String, val orderExternalId: String, val createdAt: LocalDateTime, val amount: BigDecimal)

