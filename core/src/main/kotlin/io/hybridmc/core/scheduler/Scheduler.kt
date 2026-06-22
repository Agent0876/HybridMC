package io.hybridmc.core.scheduler

/**
 * A scheduler that handles scheduling of synchronous and asynchronous tasks.
 */
public interface Scheduler {
    /**
     * Schedules a task to run synchronously on the next server tick.
     */
    public fun runTask(task: () -> Unit): ScheduledTask

    /**
     * Schedules a task to run synchronously after the specified number of ticks.
     *
     * @param delayTicks The number of ticks to delay execution.
     * @param task The task to execute.
     * @return The scheduled task handle.
     */
    public fun runTaskLater(
        delayTicks: Long,
        task: () -> Unit,
    ): ScheduledTask

    /**
     * Schedules a task to run synchronously and repeatedly.
     *
     * @param delayTicks The number of ticks to delay before the first execution.
     * @param periodTicks The number of ticks between consecutive executions.
     * @param task The task to execute.
     * @return The scheduled task handle.
     */
    public fun runTaskTimer(
        delayTicks: Long,
        periodTicks: Long,
        task: () -> Unit,
    ): ScheduledTask

    /**
     * Schedules a task to run asynchronously on a background thread.
     *
     * @param task The task to execute.
     * @return The scheduled task handle.
     */
    public fun runTaskAsynchronously(task: () -> Unit): ScheduledTask
}
