package br.dev.order.schedule

import br.dev.order.entity.OrderEntity
import br.dev.order.entity.OrderStatus
import mu.KotlinLogging
import org.jetbrains.exposed.sql.update
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class TransitionOrderToPendingSchedule {

    private val logger = KotlinLogging.logger {  }

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    fun transitionToPending() {
        logger.info { "Transition Orders to PENDING" }

        OrderEntity.update({ OrderEntity.status eq OrderStatus.CREATED }) {
            it[status] = OrderStatus.PENDING
            it[updatedAt] = LocalDateTime.now()
        }
    }
}