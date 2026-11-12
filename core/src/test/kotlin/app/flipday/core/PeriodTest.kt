package app.flipday.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PeriodTest {
    private val start = LocalDate.of(2026, 11, 2)   // Monday
    private val end = LocalDate.of(2026, 11, 6)     // Friday: 5 days, both ends count
    private fun period(title: String = "Course", s: LocalDate = start, e: LocalDate = end) =
        Event(title = title, type = EventType.PERIOD, date = s, endDate = e)

    @Test fun `first and last day both count`() {
        assertEquals(5, EventCalculator.period(start, end, start).totalDays)
    }

    @Test fun `upcoming period counts days to start`() {
        val s = EventCalculator.period(start, end, start.minusDays(3))
        assertEquals(PeriodPhase.UPCOMING, s.phase)
        assertEquals(3, s.daysToStart)
        assertEquals(0.0, s.progress)
    }

    @Test fun `first day is day 1 and the last day has zero days left`() {
        val first = EventCalculator.period(start, end, start)
        assertEquals(PeriodPhase.ONGOING, first.phase)
        assertEquals(1, first.dayNumber); assertEquals(4, first.daysLeft)
        val last = EventCalculator.period(start, end, end)
        assertEquals(PeriodPhase.ONGOING, last.phase)
        assertEquals(5, last.dayNumber); assertEquals(0, last.daysLeft); assertEquals(1.0, last.progress)
    }

    @Test fun `ended period reports how long it lasted and how long ago it ended`() {
        val s = EventCalculator.period(start, end, end.plusDays(10))
        assertEquals(PeriodPhase.ENDED, s.phase)
        assertEquals(5, s.totalDays); assertEquals(10, s.daysSinceEnd)
    }

    @Test fun `single day period is one day long`() {
        val d = LocalDate.of(2026, 5, 1)
        assertEquals(1, EventCalculator.period(d, d, d).totalDays)
        assertEquals(PeriodPhase.ONGOING, EventCalculator.period(d, d, d).phase)
    }

    @Test fun `period needs an end and the end cannot precede the start`() {
        assertFailsWith<IllegalArgumentException> { Event(title = "x", type = EventType.PERIOD, date = start) }
        assertFailsWith<IllegalArgumentException> { period(e = start.minusDays(1)) }
        assertFailsWith<IllegalArgumentException> { Event(title = "x", type = EventType.COUNTUP, date = start, endDate = end) }
    }

    @Test fun `marking a count-up as ended turns it into a period that lasted until today`() {
        val up = Event(title = "No sugar", type = EventType.COUNTUP, date = start, milestoneEveryDays = 100)
        val done = EventCalculator.markEnded(up, start.plusDays(44))
        assertEquals(EventType.PERIOD, done.type)
        assertEquals(45, EventCalculator.period(done.date, done.endDate!!, start.plusDays(50)).totalDays)
        assertEquals(null, done.milestoneEveryDays)
        assertFailsWith<IllegalArgumentException> { EventCalculator.markEnded(done, start) }
    }

    @Test fun `reminders count back from the last day`() {
        val e = period().copy(reminders = listOf(ReminderRule(1)))
        val t = ReminderPlanner.upcoming(e, start.atTime(8, 0)).single()
        assertEquals(end.minusDays(1).atTime(9, 0), t.at)
    }

    @Test fun `ended period archives after the chosen days, never when archiving is off`() {
        val e = period()
        assertEquals(false, ArchivePolicy.shouldArchive(e, end.plusDays(7), 7))
        assertEquals(true, ArchivePolicy.shouldArchive(e, end.plusDays(8), 7))
        assertEquals(false, ArchivePolicy.shouldArchive(e, end.plusDays(400), ArchivePolicy.NEVER))
        assertEquals(false, ArchivePolicy.shouldArchive(period(s = start, e = end).copy(archivedOn = end), end.plusDays(30), 7))
    }

    @Test fun `periods sort by start while ahead, then by end, and ended ones are completed`() {
        val today = LocalDate.of(2026, 11, 12)
        val ahead = period("Ahead", today.plusDays(5), today.plusDays(9))
        val running = period("Running", today.minusDays(3), today.plusDays(2))
        val over = period("Over", today.minusDays(9), today.minusDays(4))
        val order = EventQueries.active(listOf(ahead, running, over), EventQuery(showCompleted = true), today).map { it.title }
        assertEquals(listOf("Running", "Ahead", "Over"), order)
        assertTrue(over !in EventQueries.active(listOf(ahead, running, over), EventQuery(), today))
    }
}
