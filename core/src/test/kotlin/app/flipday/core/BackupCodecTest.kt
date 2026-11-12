package app.flipday.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupCodecTest {
    private val day = LocalDate.of(2026, 11, 12)
    private val sample = listOf(
        Event(title = "Baby \"Noor\" \\ due\nline", type = EventType.COUNTDOWN, date = LocalDate.of(2026, 12, 5), time = LocalTime.of(12, 0),
            startDate = day, reminders = listOf(ReminderRule(1), ReminderRule(7, LocalTime.of(8, 30))), repeatYearly = true),
        Event(title = "رحلة إسطنبول", type = EventType.PERIOD, date = LocalDate.of(2027, 1, 19), endDate = LocalDate.of(2027, 1, 25), archivedOn = day),
        Event(title = "Graduation", type = EventType.COUNTUP, date = LocalDate.of(2024, 6, 1), milestoneEveryDays = 1000),
    )

    @Test fun roundTripKeepsEverythingExceptIds() {
        val back = BackupCodec.decode(BackupCodec.encode(sample, day))
        assertEquals(0, back.skipped)
        assertEquals(sample, back.events)
    }

    @Test fun emptyListRoundTrips() = assertEquals(emptyList(), BackupCodec.decode(BackupCodec.encode(emptyList(), day)).events)

    @Test fun rejectsOtherJsonAndGarbage() {
        assertFailsWith<BackupException> { BackupCodec.decode("""{"app":"other","version":1,"events":[]}""") }
        assertFailsWith<BackupException> { BackupCodec.decode("not json") }
        assertFailsWith<BackupException> { BackupCodec.decode("[1,2]") }
        assertFailsWith<BackupException> { BackupCodec.decode("""{"app":"flipday","version":1,"events":[]} trailing""") }
    }

    @Test fun rejectsNewerVersion() {
        assertFailsWith<BackupException> { BackupCodec.decode("""{"app":"flipday","version":2,"events":[]}""") }
    }

    @Test fun badEventsAreSkippedAndCounted() {
        val json = """{"app":"flipday","version":1,"events":[{"title":"","type":"COUNTDOWN","date":"2026-01-01"},{"title":"ok","type":"COUNTUP","date":"2026-01-01"},{"title":"x","type":"NOPE","date":"2026-01-01"}]}"""
        val r = BackupCodec.decode(json)
        assertEquals(listOf("ok"), r.events.map { it.title })
        assertEquals(2, r.skipped)
    }

    @Test fun sameEventIgnoresIdsAndReminders() {
        val a = sample[2]
        assertTrue(BackupCodec.sameEvent(a, a.copy(id = 9, reminders = emptyList())))
    }
}
