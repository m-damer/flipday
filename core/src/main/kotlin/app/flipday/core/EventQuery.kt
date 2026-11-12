package app.flipday.core

import java.time.LocalDate

enum class EventSort { NEAREST, DATE_ADDED, NAME }

/** What the Events list (and Search, and Archive) shows. [type] null means every type. */
data class EventQuery(
    val sort: EventSort = EventSort.NEAREST,
    val type: EventType? = null,
    val showCompleted: Boolean = false,
    val text: String = "",
)

/** Pure list logic for Events, Search and Archive. Matches design board 06C.6 (Sort and filter). */
object EventQueries {

    /** A countdown whose date has passed (and does not repeat), or a period that has ended. Not yet archived. */
    fun isCompleted(event: Event, today: LocalDate): Boolean = when (event.type) {
        EventType.COUNTDOWN -> !event.repeatYearly && event.date.isBefore(today)
        EventType.PERIOD -> event.endDate?.isBefore(today) == true
        else -> false
    }

    /** The next date that matters: a period's start while it is ahead, then its end. */
    private fun sortDate(e: Event, today: LocalDate): LocalDate? =
        if (e.type == EventType.PERIOD && today.isBefore(e.date)) e.date else ReminderPlanner.anchorDate(e, today)

    /** Active events: not archived, completed ones only when [EventQuery.showCompleted]. */
    fun active(events: List<Event>, q: EventQuery, today: LocalDate): List<Event> =
        sorted(events.filter { it.archivedOn == null }.filter { matches(it, q, today) && (q.showCompleted || !isCompleted(it, today)) }, q.sort, today)

    /** Search looks at active and archived events, completed included. */
    fun search(events: List<Event>, text: String, today: LocalDate): List<Event> {
        if (text.isBlank()) return emptyList()
        return sorted(events.filter { matches(it, EventQuery(text = text), today) }, EventSort.NEAREST, today)
    }

    fun archived(events: List<Event>, type: EventType?): List<Event> =
        events.filter { it.archivedOn != null && (type == null || it.type == type) }.sortedByDescending { it.archivedOn }

    private fun matches(e: Event, q: EventQuery, today: LocalDate): Boolean =
        (q.type == null || e.type == q.type) && (q.text.isBlank() || e.title.contains(q.text.trim(), ignoreCase = true))

    /** Nearest first: upcoming dates ascending, then everything else (count-ups, completed) newest date first. */
    private fun sorted(list: List<Event>, sort: EventSort, today: LocalDate): List<Event> = when (sort) {
        EventSort.NAME -> list.sortedBy { it.title.lowercase() }
        EventSort.DATE_ADDED -> list.sortedByDescending { it.id }
        EventSort.NEAREST -> {
            val (upcoming, rest) = list.partition { sortDate(it, today)?.let { d -> !d.isBefore(today) } == true && !isCompleted(it, today) }
            upcoming.sortedBy { sortDate(it, today) } + rest.sortedByDescending { it.date }
        }
    }
}
