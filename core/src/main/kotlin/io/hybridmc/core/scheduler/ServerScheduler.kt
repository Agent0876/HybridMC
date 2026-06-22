package io.hybridmc.core.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Concrete implementation of the [Scheduler] for HybridMC.
 */
public class ServerScheduler : Scheduler {
    private val nextTaskId = AtomicLong(0)
    private val syncTasks = CopyOnWriteArrayList<SyncTask>()

    private val asyncExecutor: ExecutorService =
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable).apply {
                isDaemon = true
                name = "hybridmc-scheduler-async"
            }
        }

    @Volatile
    private var currentTickCount: Long = 0

    /**
     * Advances the scheduler tick, executing any pending synchronous tasks whose execution tick has arrived.
     * This must be called from the server's main thread.
     */
    public fun tick(tickCount: Long) {
        currentTickCount = tickCount
        if (syncTasks.isEmpty()) return

        val toRemove = mutableListOf<SyncTask>()
        for (task in syncTasks) {
            if (task.isCancelled) {
                toRemove.add(task)
                continue
            }
            if (task.scheduledTick <= tickCount) {
                try {
                    task.task()
                } catch (t: Throwable) {
                    logger.error(t) { "Error executing scheduled task #${task.id}" }
                }

                val period = task.period
                if (period > 0) {
                    task.scheduledTick += period
                } else {
                    toRemove.add(task)
                }
            }
        }

        if (toRemove.isNotEmpty()) {
            syncTasks.removeAll(toRemove)
        }
    }

    /**
     * Shuts down the asynchronous task executor pool.
     */
    public fun shutdown() {
        asyncExecutor.shutdown()
    }

    override fun runTask(task: () -> Unit): ScheduledTask = runTaskLater(0, task)

    override fun runTaskLater(
        delayTicks: Long,
        task: () -> Unit,
    ): ScheduledTask {
        val delay = if (delayTicks < 0) 0 else delayTicks
        val targetTick = currentTickCount + delay
        val scheduledTask =
            SyncTask(
                id = nextTaskId.getAndIncrement(),
                scheduledTick = targetTick,
                period = -1,
                task = task,
            )
        syncTasks.add(scheduledTask)
        return scheduledTask
    }

    override fun runTaskTimer(
        delayTicks: Long,
        periodTicks: Long,
        task: () -> Unit,
    ): ScheduledTask {
        val delay = if (delayTicks < 0) 0 else delayTicks
        val period = if (periodTicks <= 0) 1 else periodTicks
        val targetTick = currentTickCount + delay
        val scheduledTask =
            SyncTask(
                id = nextTaskId.getAndIncrement(),
                scheduledTick = targetTick,
                period = period,
                task = task,
            )
        syncTasks.add(scheduledTask)
        return scheduledTask
    }

    override fun runTaskAsynchronously(task: () -> Unit): ScheduledTask {
        val id = nextTaskId.getAndIncrement()
        val future =
            asyncExecutor.submit {
                try {
                    task()
                } catch (t: Throwable) {
                    logger.error(t) { "Error executing asynchronous task #$id" }
                }
            }
        return AsyncTask(id, future)
    }

    private class SyncTask(
        override val id: Long,
        @Volatile var scheduledTick: Long,
        val period: Long,
        val task: () -> Unit,
    ) : ScheduledTask {
        @Volatile
        private var cancelled = false

        override val isAsync: Boolean get() = false
        override val isCancelled: Boolean get() = cancelled

        override fun cancel() {
            cancelled = true
        }
    }

    private class AsyncTask(
        override val id: Long,
        private val future: Future<*>,
    ) : ScheduledTask {
        override val isAsync: Boolean get() = true
        override val isCancelled: Boolean get() = future.isCancelled

        override fun cancel() {
            future.cancel(true)
        }
    }
}
