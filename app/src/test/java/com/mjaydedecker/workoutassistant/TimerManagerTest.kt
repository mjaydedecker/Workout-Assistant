package com.mjaydedecker.workoutassistant

import com.mjaydedecker.workoutassistant.util.TimerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    @Test
    fun `startOrReset sets initial values synchronously`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 30)
        // Values are set synchronously before launching coroutines
        assertEquals(10L, timer.restTimeRemaining.value)
        assertEquals(30L, timer.inactivityTimeRemaining.value)
    }

    @Test
    fun `countdown decrements each second`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 20)
        // +1ms ensures the task scheduled at exactly 3000ms is included
        advanceTimeBy(3001L)
        assertEquals(7L, timer.restTimeRemaining.value)
        assertEquals(17L, timer.inactivityTimeRemaining.value)
    }

    @Test
    fun `cancel resets timers to idle state`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 30)
        timer.cancel()
        assertEquals(-1L, timer.restTimeRemaining.value)
        assertEquals(-1L, timer.inactivityTimeRemaining.value)
    }

    @Test
    fun `pause stops countdown`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 20)
        advanceTimeBy(3001L)
        val restAtPause = timer.restTimeRemaining.value   // 7
        timer.pause()
        advanceTimeBy(5000L) // time passes while paused — counters must not change
        assertEquals(restAtPause, timer.restTimeRemaining.value)
    }

    @Test
    fun `resume continues countdown from paused value`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 20)
        advanceTimeBy(3001L)                        // 3 ticks → rest=7
        val restAtPause = timer.restTimeRemaining.value  // 7
        timer.pause()
        advanceTimeBy(5000L)                        // paused — no change
        timer.resume()
        advanceTimeBy(2001L)                        // 2 more ticks after resume
        assertEquals(restAtPause - 2L, timer.restTimeRemaining.value)  // 5
    }

    @Test
    fun `startOrReset after pause clears paused state and resets to full duration`() = runTest {
        val timer = TimerManager(this)
        timer.startOrReset(10, 20)
        advanceTimeBy(3001L)
        timer.pause()
        timer.startOrReset(10, 20) // reset — should restart from full duration
        assertEquals(10L, timer.restTimeRemaining.value)
        assertEquals(20L, timer.inactivityTimeRemaining.value)
    }

    @Test
    fun `onRestExpired fires when rest timer reaches zero`() = runTest {
        val timer = TimerManager(this)
        var fired = false
        timer.onRestExpired = { fired = true }
        timer.startOrReset(3, 100)
        advanceTimeBy(3001L)
        assertTrue(fired)
    }

    @Test
    fun `onInactivityExpired fires when inactivity timer reaches zero`() = runTest {
        val timer = TimerManager(this)
        var fired = false
        timer.onInactivityExpired = { fired = true }
        timer.startOrReset(100, 3)
        advanceTimeBy(3001L)
        assertTrue(fired)
    }

    @Test
    fun `expiry callbacks do not fire when paused`() = runTest {
        val timer = TimerManager(this)
        var restFired = false
        var inactivityFired = false
        timer.onRestExpired = { restFired = true }
        timer.onInactivityExpired = { inactivityFired = true }
        timer.startOrReset(5, 5)
        advanceTimeBy(2000L)
        timer.pause()
        advanceTimeBy(10000L) // more than enough to expire if running
        assertFalse(restFired)
        assertFalse(inactivityFired)
    }
}
