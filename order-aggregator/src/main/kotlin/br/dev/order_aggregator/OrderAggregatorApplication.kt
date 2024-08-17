package br.dev.order_aggregator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrderAggregatorApplication

fun main(args: Array<String>) {
	runApplication<OrderAggregatorApplication>(*args)
}