package io.hybridmc.core.scheduler

/**
 * Represents a task that has been scheduled for execution.
 */
public interface ScheduledTask {
    /** The unique identifier of this task. */
    public val id: Long

    /** Whether the task runs asynchronously. */
    public val isAsync: Boolean

    /** Whether the task has been cancelled. */
    public val isCancelled: Boolean

    /**
     * Cancels the execution of this task.
     * If the task is repeating, it will not run again.
     * If the task is scheduled but has not yet run, it is removed from the execution queue.
     */
    public fun cancel()
}
