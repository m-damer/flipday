package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventCalculatorTest {
    private val today = LocalDate.of(2026, 11, 12)

    @Test fun daysUntilDueDate() = assertEquals(23, EventCalculator.daysUntil(LocalDate.of(2026, 12, 5), today))
    @Test fun daysUntilTrip() = assertEquals(68, EventCalculator.daysUntil(LocalDate.of(2027, 1, 19), today))
    @Test fun daysSinceGraduation() = assertEquals(894, EventCalculator.daysSince(LocalDate.of(2024, 6, 1), today))
    @Test fun milestoneIn106() = assertEquals(106, EventCalculator.daysToMilestone(894))
    @Test fun ageOfLayla() = assertEquals(Ymd(6, 10, 20), EventCalculator.age(LocalDate.of(2019, 12, 23), today))
    @Test fun nextBirthdayIn41Days() {
        val birth = LocalDate.of(2019, 12, 23)
        assertEquals(41, EventCalculator.daysUntil(EventCalculator.nextBirthday(birth, today), today))
        assertEquals(7, EventCalculator.turnsAge(birth, today))
    }
    @Test fun birthdayToday() = assertEquals(today, EventCalculator.nextBirthday(LocalDate.of(2000, 11, 12), today))
    @Test fun leapDayBirthdayFallsOn28thInCommonYear() =
        assertEquals(LocalDate.of(2027, 2, 28), EventCalculator.nextBirthday(LocalDate.of(2020, 2, 29), today))
    @Test fun phases() {
        assertEquals(EventPhase.TODAY, EventCalculator.phase(today, today))
        assertEquals(EventPhase.DONE, EventCalculator.phase(today.minusDays(1), today))
        assertEquals(EventPhase.UPCOMING, EventCalculator.phase(today.plusDays(1), today))
    }
    @Test fun progressIsClamped() {
        assertNull(EventCalculator.progress(null, today, today))
        assertEquals(1.0, EventCalculator.progress(today.minusDays(10), today.minusDays(1), today))
    }
    @Test fun remainingTimeNeverNegative() =
        assertEquals(Remaining(0, 0, 0), EventCalculator.remaining(LocalDateTime.of(2026, 11, 12, 8, 0), LocalDateTime.of(2026, 11, 12, 9, 0)))
    @Test fun remainingSplitsDaysHoursMinutes() =
        assertEquals(Remaining(23, 5, 52), EventCalculator.remaining(LocalDateTime.of(2026, 12, 5, 12, 0), LocalDateTime.of(2026, 11, 12, 6, 8)))
}
