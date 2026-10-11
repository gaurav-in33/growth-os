package com.growthos.personal.data

/**
 * Streak rules (shown to the user in the Track tab):
 * - Counts consecutive days with a check-in, ending today (or yesterday if today is not done yet).
 * - One missed day is forgiven (a "recovery day") if the day before it was done,
 *   and at most once in any 7-day stretch. Recovery days do not add to the count.
 */
object StreakCalculator {
    fun streak(doneDays: Set<Long>, today: Long): Int {
        var day = if (today in doneDays) today else today - 1
        var count = 0
        var lastRecoveryDay = Long.MAX_VALUE
        while (true) {
            if (day in doneDays) {
                count++
                day--
                continue
            }
            val previousDone = (day - 1) in doneDays
            val recoveryAllowed = lastRecoveryDay == Long.MAX_VALUE || lastRecoveryDay - day >= 7
            if (previousDone && recoveryAllowed) {
                lastRecoveryDay = day
                day--
                continue
            }
            break
        }
        return count
    }
}
