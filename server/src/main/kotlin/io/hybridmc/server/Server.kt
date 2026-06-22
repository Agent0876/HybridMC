package io.hybridmc.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.hybridmc.core.event.EventBus
import io.hybridmc.core.event.ServerTickEvent
import io.hybridmc.core.scheduler.Scheduler
import io.hybridmc.core.scheduler.ServerScheduler
import io.hybridmc.core.service.ServiceRegistry
import io.hybridmc.core.service.Subsystem

private val logger = KotlinLogging.logger {}

/**
 * Owns the lifecycle and the 20 TPS tick loop. It only knows the [Subsystem] SPI,
 * so it never depends on concrete domain modules — those are wired in by the composition root.
 */
public class Server(
    private val services: ServiceRegistry,
    private val subsystems: List<Subsystem>,
) {
    @Volatile
    private var running = false

    /** Returns true if the server is currently running. */
    public val isRunning: Boolean get() = running

    /**
     * Starts the server. This blocks the calling thread, running the 20 TPS game loop
     * until [stop] is called.
     */
    public fun start() {
        if (running) return
        running = true

        val scheduler = ServerScheduler()
        services.register(Scheduler::class, scheduler)

        logger.info { "Starting server with ${subsystems.size} subsystem(s)" }
        subsystems.forEach { it.start(services) }

        // Freeze all registries after subsystems have registered their entries
        val freezer = services.find(io.hybridmc.core.registry.RegistryFreezer::class)
        freezer?.freezeAll()

        val tickDurationNs = 50_000_000L // 50ms in nanoseconds (20 TPS)
        var lastTickTimeNs = System.nanoTime()
        var accumulatedTimeNs = 0L
        var tickCount = 0L

        // TPS Tracking
        var tpsLastCheckedNs = System.nanoTime()
        var tpsTickCount = 0
        var currentTps = 20.0

        val eventBus = services.find(EventBus::class)

        try {
            while (running) {
                val now = System.nanoTime()
                val elapsed = now - lastTickTimeNs
                lastTickTimeNs = now
                accumulatedTimeNs += elapsed

                var ticksThisCycle = 0
                val maxCatchUpTicks = 10

                // Tick catch-up loop
                while (accumulatedTimeNs >= tickDurationNs && ticksThisCycle < maxCatchUpTicks) {
                    tickCount++
                    tpsTickCount++
                    ticksThisCycle++
                    accumulatedTimeNs -= tickDurationNs

                    // 1. Tick the scheduler
                    scheduler.tick(tickCount)

                    // 2. Dispatch ServerTickEvent
                    eventBus?.post(ServerTickEvent(tickCount))
                }

                // Discard excess time if we hit catch-up cap to prevent runaway death spiral
                if (accumulatedTimeNs >= tickDurationNs) {
                    val discardedTicks = accumulatedTimeNs / tickDurationNs
                    logger.warn { "Server is lagging! Discarding $discardedTicks tick(s)." }
                    accumulatedTimeNs = 0L
                }

                // Update TPS log every 5 seconds
                val tpsElapsed = now - tpsLastCheckedNs
                if (tpsElapsed >= 5_000_000_000L) {
                    currentTps = (tpsTickCount.toDouble() / tpsElapsed) * 1_000_000_000.0
                    if (currentTps > 20.0) currentTps = 20.0

                    if (currentTps < 15.0) {
                        logger.warn { "Low TPS! Running at ${"%.2f".format(currentTps)} TPS (target: 20.0)" }
                    }
                    tpsTickCount = 0
                    tpsLastCheckedNs = now
                }

                // Sleep logic to yield CPU
                if (running) {
                    val nextTickTimeNs = lastTickTimeNs + (tickDurationNs - accumulatedTimeNs)
                    val sleepTimeNs = nextTickTimeNs - System.nanoTime()
                    if (sleepTimeNs > 0) {
                        val sleepMs = sleepTimeNs / 1_000_000L
                        val sleepNs = (sleepTimeNs % 1_000_000L).toInt()
                        try {
                            Thread.sleep(sleepMs, sleepNs)
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }
        } finally {
            running = false
            scheduler.shutdown()
            subsystems.asReversed().forEach {
                try {
                    it.stop()
                } catch (t: Throwable) {
                    logger.error(t) { "Error stopping subsystem ${it.id}" }
                }
            }
            logger.info { "Server stopped" }
        }
    }

    /**
     * Signals the server tick loop to stop.
     */
    public fun stop() {
        running = false
    }
}
