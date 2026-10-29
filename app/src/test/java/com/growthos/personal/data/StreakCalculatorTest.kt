package com.growthos.personal.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun noCheckins_isZero() {
        assertEquals(0, StreakCalculator.streak(emptySet(), 100L))
    }

    @Test
    fun threeDaysEndingToday_isThree() {
        assertEquals(3, StreakCalculator.streak(setOf(98L, 99L, 100L), 100L))
    }

    @Test
    fun todayNotDoneYet_countsFromYesterday() {
        assertEquals(2, StreakCalculator.streak(setOf(98L, 99L), 100L))
    }

    @Test
    fun oneMissedDay_isForgiven() {
        // 97, 98 done, 99 missed (recovery day), 100 done
        assertEquals(3, StreakCalculator.streak(setOf(97L, 98L, 100L), 100L))
    }

    @Test
    fun twoMissedDaysInARow_breaksStreak() {
        assertEquals(1, StreakCalculator.streak(setOf(96L, 97L, 100L), 100L))
    }

    @Test
    fun secondRecoveryWithinSevenDays_breaksStreak() {
        // 100 done, 99 missed (recovery), 98 done, 97 missed (second recovery inside 7 days), 96 done
        assertEquals(2, StreakCalculator.streak(setOf(96L, 98L, 100L), 100L))
    }
}
