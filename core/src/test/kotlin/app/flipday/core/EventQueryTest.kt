package app.flipday.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventQueryTest {
    private val today = LocalDate.of(2026, 11, 12)
    private fun ev(id: Long, title: String, type: EventType, date: LocalDate, archived: LocalDate? = null, repeat: Boolean = false) =
        Event(id = id, title = title, type = type, date = date, archivedOn = archived, repeatYearly = repeat)

    private val trip = ev(1, "Trip", EventType.COUNTDOWN, today.plusDays(30))
    private val exam = ev(2, "Exam", EventType.COUNTDOWN, today.plusDays(3))
    private val past = ev(3, "Concert", EventType.COUNTDOWN, today.minusDays(2))
    private val bday = ev(4, "Noor birthday", EventType.AGE, LocalDate.of(2020, 11, 20))
    private val since = ev(5, "Quit sugar", EventType.COUNTUP, today.minusDays(40))
    private val old = ev(6, "Old trip", EventType.COUNTDOWN, today.minusDays(60), archived = today.minusDays(50))
    private val all = listOf(trip, exam, past, bday, since, old)

    @Test fun `nearest first puts upcoming ascending then the rest`() {
        val r = EventQueries.active(all, EventQuery(), today).map { it.title }
        assertEquals(listOf("Exam", "Noor birthday", "Trip", "Quit sugar"), r)
    }

    @Test fun `completed events are hidden unless asked for`() {
        assertTrue(past !in EventQueries.active(all, EventQuery(), today))
        assertTrue(past in EventQueries.active(all, EventQuery(showCompleted = true), today))
    }

    @Test fun `archived events never appear in the active list`() {
        assertTrue(old !in EventQueries.active(all, EventQuery(showCompleted = true), today))
    }

    @Test fun `repeating countdown is not completed`() {
        assertTrue(!EventQueries.isCompleted(ev(7, "Anniv", EventType.COUNTDOWN, today.minusDays(1), repeat = true), today))
    }

    @Test fun `type filter`() {
        assertEquals(listOf("Quit sugar"), EventQueries.active(all, EventQuery(type = EventType.COUNTUP), today).map { it.title })
    }

    @Test fun `sort by name and by date added`() {
        assertEquals(listOf("Exam", "Noor birthday", "Quit sugar", "Trip"), EventQueries.active(all, EventQuery(sort = EventSort.NAME), today).map { it.title })
        assertEquals(listOf(5L, 4L, 2L, 1L), EventQueries.active(all, EventQuery(sort = EventSort.DATE_ADDED), today).map { it.id })
    }

    @Test fun `search includes archived and completed and ignores case`() {
        assertEquals(listOf("Trip", "Old trip"), EventQueries.search(all, "TRIP", today).map { it.title })
        assertTrue(EventQueries.search(all, "  ", today).isEmpty())
        assertTrue(EventQueries.search(all, "wedding", today).isEmpty())
    }

    @Test fun `archive list is newest archived first and filterable`() {
        val older = ev(8, "Older", EventType.COUNTUP, today.minusDays(400), archived = today.minusDays(100))
        assertEquals(listOf("Old trip", "Older"), EventQueries.archived(all + older, null).map { it.title })
        assertEquals(listOf("Older"), EventQueries.archived(all + older, EventType.COUNTUP).map { it.title })
    }
}
