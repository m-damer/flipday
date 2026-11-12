package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPlannerTest {
    private val now = LocalDateTime.of(2026, 11, 12, 10, 0)
    private val launch = Event(
        title = "Project launch", type = EventType.COUNTDOWN, date = LocalDate.of(2026, 11, 21),
        reminders = listOf(ReminderRule(7), ReminderRule(1), ReminderRule(0, LocalTime.of(12, 0))),
    )

    @Test fun skipsPastTriggersAndSorts() {
        val t = ReminderPlanner.upcoming(launch, now)
        assertEquals(listOf(LocalDateTime.of(2026, 11, 14, 9, 0), LocalDateTime.of(2026, 11, 20, 9, 0), LocalDateTime.of(2026, 11, 21, 12, 0)), t.map { it.at })
    }
    @Test fun dropsTriggerAlreadyPassedToday() {
        val e = launch.copy(date = LocalDate.of(2026, 11, 12), reminders = listOf(ReminderRule(0, LocalTime.of(9, 0))))
        assertTrue(ReminderPlanner.upcoming(e, now).isEmpty())
    }
    @Test fun birthdayUsesNextOccurrence() {
        val b = Event(title = "Layla", type = EventType.AGE, date = LocalDate.of(2019, 12, 23), reminders = listOf(ReminderRule(1)))
        assertEquals(LocalDateTime.of(2026, 12, 22, 9, 0), ReminderPlanner.upcoming(b, now).single().at)
    }
    @Test fun countUpHasNoDateReminders() {
        val c = Event(title = "Graduation", type = EventType.COUNTUP, date = LocalDate.of(2024, 6, 1), reminders = listOf(ReminderRule(1)))
        assertTrue(ReminderPlanner.upcoming(c, now).isEmpty())
    }
    @Test fun milestoneEvery100Days() {
        val c = Event(title = "Graduation", type = EventType.COUNTUP, date = LocalDate.of(2024, 6, 1), milestoneEveryDays = 100)
        // 894 days since start, so the next multiple of 100 is day 900.
        assertEquals(LocalDate.of(2024, 6, 1).plusDays(900), ReminderPlanner.milestoneTrigger(c, now.toLocalDate())!!.at.toLocalDate())
    }
    @Test fun milestoneOffReturnsNull() =
        assertNull(ReminderPlanner.milestoneTrigger(Event(title = "x", type = EventType.COUNTUP, date = LocalDate.of(2024, 6, 1)), now.toLocalDate()))
}
