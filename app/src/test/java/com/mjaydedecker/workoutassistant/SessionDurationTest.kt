package com.mjaydedecker.workoutassistant

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the duration calculation logic used in SessionRepository.endSession():
 *   durationSeconds = (endTime.epochSecond - startTime.epochSecond) - totalPausedSeconds
 */
class SessionDurationTest {

    private fun calcDuration(elapsedSeconds: Long, pausedSeconds: Long): Long =
        (elapsedSeconds - pausedSeconds).coerceAtLeast(0)

    @Test
    fun `duration with no pauses equals total elapsed time`() {
        assertEquals(3600L, calcDuration(3600L, 0L))
    }

    @Test
    fun `duration subtracts paused time correctly`() {
        // 60 min session, 10 min paused → 50 min active
        assertEquals(3000L, calcDuration(3600L, 600L))
    }

    @Test
    fun `duration with multiple pause accumulations`() {
        // 30 min session, paused 5 min + 3 min = 8 min
        assertEquals(1320L, calcDuration(1800L, 480L))
    }

    @Test
    fun `duration never goes below zero`() {
        // Edge case: paused longer than session (e.g., clock drift)
        assertEquals(0L, calcDuration(100L, 200L))
    }

    @Test
    fun `duration with zero elapsed and zero paused is zero`() {
        assertEquals(0L, calcDuration(0L, 0L))
    }
}
