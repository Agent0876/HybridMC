package io.hybridmc.server

import io.hybridmc.core.event.EventBus
import io.hybridmc.core.event.ServerTickEvent
import io.hybridmc.core.scheduler.Scheduler
import io.hybridmc.core.service.SimpleServiceRegistry
import io.hybridmc.core.service.Subsystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class TickLoopTest {
    @Test
    fun `test server tick loop execution and stop`() {
        val registry = SimpleServiceRegistry()
        val eventBus = EventBus()
        registry.register(EventBus::class, eventBus)

        var subsystemStarted = false
        var subsystemStopped = false

        val testSubsystem =
            object : Subsystem {
                override val id: String = "test-subsystem"

                override fun start(services: io.hybridmc.core.service.ServiceRegistry) {
                    subsystemStarted = true
                }

                override fun stop() {
                    subsystemStopped = true
                }
            }

        // Setup scheduler helper subsystem to trigger shutdown after 5 ticks
        val schedulerSubsystem =
            object : Subsystem {
                override val id: String = "scheduler-subsystem"

                override fun start(services: io.hybridmc.core.service.ServiceRegistry) {
                    val scheduler = services.get(Scheduler::class)
                    scheduler.runTaskLater(5) {
                        // Trigger stop after 5 ticks
                        val target = services.get(Server::class)
                        target.stop()
                    }
                }

                override fun stop() {}
            }

        // Register Server so the subsystem can find it to shut down
        val subsystems = listOf(testSubsystem, schedulerSubsystem)
        val server = Server(registry, subsystems)
        registry.register(Server::class, server)

        val ticksCount = AtomicInteger(0)
        eventBus.register(ServerTickEvent::class) { event ->
            ticksCount.incrementAndGet()
        }

        assertFalse(server.isRunning)

        val startTime = System.currentTimeMillis()
        server.start()
        val duration = System.currentTimeMillis() - startTime

        assertFalse(server.isRunning)
        assertTrue(subsystemStarted)
        assertTrue(subsystemStopped)

        // 5 ticks of 50ms should take at least ~250ms
        assertTrue(duration >= 200, "Tick loop should block for approximately 250ms (got ${duration}ms)")
        assertEquals(5, ticksCount.get(), "Should execute exactly 5 ticks before stopping")
    }
}
