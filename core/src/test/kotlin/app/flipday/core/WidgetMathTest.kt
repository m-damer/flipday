package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetMathTest {
    private val today = LocalDate.of(2026, 11, 12)

    @Test fun countdownRingUsesStartDate() {
        val e = Event(title = "A", type = EventType.COUNTDOWN, date = today.plusDays(10), startDate = today.minusDays(10))
        assertEquals(0.5f, WidgetMath.ringProgress(e, today))
    }

    @Test fun countdownWithoutStartHasEmptyRing() {
        assertEquals(0f, WidgetMath.ringProgress(Event(title = "A", type = EventType.COUNTDOWN, date = today.plusDays(5)), today))
    }

    @Test fun periodRingIsDayNumberOverLength() {
        val e = Event(title = "P", type = EventType.PERIOD, date = today.minusDays(1), endDate = today.plusDays(2))
        assertEquals(0.5f, WidgetMath.ringProgress(e, today))
    }

    @Test fun countUpRingStaysEmpty() {
        assertEquals(0f, WidgetMath.ringProgress(Event(title = "C", type = EventType.COUNTUP, date = today.minusDays(100)), today))
    }

    @Test fun tickerSplitsDaysHoursMinutes() {
        val e = Event(title = "A", type = EventType.COUNTDOWN, date = today.plusDays(2), time = LocalTime.of(10, 30))
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        assertEquals(Triple(2L, 1, 30), WidgetMath.tickerParts(e, today, now))
    }

    @Test fun tickerHasNothingForCountUpOrPastTarget() {
        val now = LocalDateTime.of(today, LocalTime.NOON)
        assertNull(WidgetMath.tickerParts(Event(title = "C", type = EventType.COUNTUP, date = today.minusDays(3)), today, now))
        assertNull(WidgetMath.tickerParts(Event(title = "A", type = EventType.COUNTDOWN, date = today, time = LocalTime.of(8, 0)), today, now))
    }

    @Test fun yearProgressMatchesDesignExample() {
        val y = WidgetMath.yearProgress(LocalDate.of(2026, 11, 12))
        assertEquals(46, y.week); assertEquals(49, y.daysLeft)
        assertEquals(0.87f, y.progress, 0.01f)
    }

    @Test fun yearProgressLastDayHasNoDaysLeft() {
        val y = WidgetMath.yearProgress(LocalDate.of(2026, 12, 31))
        assertEquals(52, y.week); assertEquals(0, y.daysLeft); assertEquals(1f, y.progress)
    }

    @Test fun milestoneAfter894DaysIs1000() {
        val m = WidgetMath.nextMilestone(894)
        assertEquals(1000L, m.target); assertEquals(106L, m.daysLeft); assertEquals(listOf(365L, 730L, 1000L), m.shown)
    }

    @Test fun milestoneBeyondFixedStepsRoundsUpToThousands() {
        val m = WidgetMath.nextMilestone(1200)
        assertEquals(2000L, m.target); assertEquals(800L, m.daysLeft)
    }

    @Test fun milestoneEarlyStartsAt100() {
        assertEquals(100L, WidgetMath.nextMilestone(10).target)
    }

    @Test fun livedDaysAndWeeks() {
        val (d, w) = WidgetMath.lived(LocalDate.of(2019, 12, 23), LocalDate.of(2026, 11, 12))
        assertEquals(2516L, d); assertEquals(359L, w)
    }

    @Test fun monthGridDecember2026StartsOnTuesdayWithMondayStart() {
        val g = WidgetMath.monthGrid(LocalDate.of(2026, 12, 5), java.time.DayOfWeek.MONDAY)
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), g.first())
        assertEquals(31, g.flatten().filterNotNull().max())
        assertEquals(true, g.all { it.size == 7 })
    }

    @Test fun monthGridSundayStartShiftsLeadingBlanks() {
        val g = WidgetMath.monthGrid(LocalDate.of(2026, 12, 5), java.time.DayOfWeek.SUNDAY)
        assertEquals(listOf(null, null, 1, 2, 3, 4, 5), g.first())
    }
}
