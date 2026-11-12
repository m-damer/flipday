package app.flipday.data

import app.flipday.core.ArchivePolicy
import app.flipday.core.Event
import app.flipday.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class EventRepository(private val dao: EventDao, private val scheduler: ReminderScheduler) {
    fun observeEvents(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: Long): Flow<Event?> = observeEvents().map { list -> list.firstOrNull { it.id == id } }

    suspend fun get(id: Long): Event? = dao.getById(id)?.toDomain()

    suspend fun all(): List<Event> = dao.getAll().map { it.toDomain() }

    suspend fun save(event: Event): Long {
        val id = dao.upsert(event.toEntity())
        if (event.archivedOn == null) scheduler.schedule(event.copy(id = id)) else scheduler.cancel(id)
        return id
    }

    suspend fun archive(event: Event, today: LocalDate) { save(event.copy(archivedOn = today)) }

    suspend fun restore(event: Event) { save(event.copy(archivedOn = null)) }

    suspend fun duplicate(event: Event, newTitle: String): Long =
        save(event.copy(id = 0, title = newTitle, archivedOn = null))

    /** Moves finished countdowns to the archive. Run on app start. */
    suspend fun archiveFinished(today: LocalDate, afterDays: Int) {
        all().filter { ArchivePolicy.shouldArchive(it, today, afterDays) }.forEach { archive(it, today) }
    }

    suspend fun delete(event: Event) {
        scheduler.cancel(event.id)
        dao.delete(event.toEntity())
    }

    /** Called after boot, time change or app update, when alarms are lost. */
    suspend fun rescheduleAll() = all().filter { it.archivedOn == null }.forEach { scheduler.schedule(it) }
}
