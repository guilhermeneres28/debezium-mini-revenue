package br.dev.order

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@ImportAutoConfiguration(ExposedAutoConfiguration::class)
@SpringBootApplication
class OrderApplication

fun main(args: Array<String>) {
	runApplication<OrderApplication>(*args)
}

