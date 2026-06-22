package io.hybridmc.core.scheduler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SchedulerTest {
    @Test
    fun `test run task on next tick`() {
        val scheduler = ServerScheduler()
        var runCount = 0

        scheduler.runTask {
            runCount++
        }

        assertEquals(0, runCount)
        scheduler.tick(0)
        assertEquals(1, runCount)

        // Ticking again should not run it again
        scheduler.tick(1)
        assertEquals(1, runCount)
    }

    @Test
    fun `test run task later with delay`() {
        val scheduler = ServerScheduler()
        var runCount = 0

        scheduler.runTaskLater(5) {
            runCount++
        }

        // Ticks 0 to 4 should not trigger the task
        for (t in 0L..4L) {
            scheduler.tick(t)
            assertEquals(0, runCount)
        }

        // Tick 5 should trigger it
        scheduler.tick(5)
        assertEquals(1, runCount)

        // Tick 6 should not trigger it again
        scheduler.tick(6)
        assertEquals(1, runCount)
    }

    @Test
    fun `test run task timer repeating`() {
        val scheduler = ServerScheduler()
        var runCount = 0

        // Delay: 2 ticks, Period: 3 ticks
        scheduler.runTaskTimer(2, 3) {
            runCount++
        }

        scheduler.tick(0) // tick 0
        assertEquals(0, runCount)
        scheduler.tick(1) // tick 1
        assertEquals(0, runCount)

        scheduler.tick(2) // tick 2 (runs first time)
        assertEquals(1, runCount)

        scheduler.tick(3) // tick 3
        scheduler.tick(4) // tick 4
        assertEquals(1, runCount)

        scheduler.tick(5) // tick 5 (runs second time: 2 + 3)
        assertEquals(2, runCount)

        scheduler.tick(6) // tick 6
        scheduler.tick(7) // tick 7
        assertEquals(2, runCount)

        scheduler.tick(8) // tick 8 (runs third time: 5 + 3)
        assertEquals(3, runCount)
    }

    @Test
    fun `test cancel synchronous task`() {
        val scheduler = ServerScheduler()
        var runCount = 0

        val task =
            scheduler.runTaskLater(5) {
                runCount++
            }

        assertFalse(task.isCancelled)
        task.cancel()
        assertTrue(task.isCancelled)

        scheduler.tick(5)
        assertEquals(0, runCount) // did not run
    }

    @Test
    fun `test cancel repeating task`() {
        val scheduler = ServerScheduler()
        var runCount = 0

        val task =
            scheduler.runTaskTimer(2, 2) {
                runCount++
            }

        scheduler.tick(2) // runs once
        assertEquals(1, runCount)

        task.cancel()

        scheduler.tick(4) // should not run
        assertEquals(1, runCount)
    }

    @Test
    fun `test run task asynchronously`() {
        val scheduler = ServerScheduler()
        val latch = CountDownLatch(1)
        var threadName: String? = null

        val task =
            scheduler.runTaskAsynchronously {
                threadName = Thread.currentThread().name
                latch.countDown()
            }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertTrue(threadName?.startsWith("hybridmc-scheduler-async") == true)
        assertFalse(task.isCancelled)

        scheduler.shutdown()
    }
}
