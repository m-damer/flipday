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
}
