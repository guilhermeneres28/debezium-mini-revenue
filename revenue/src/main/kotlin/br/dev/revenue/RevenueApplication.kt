package br.dev.revenue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RevenueApplication

fun main(args: Array<String>) {
	runApplication<RevenueApplication>(*args)
}
