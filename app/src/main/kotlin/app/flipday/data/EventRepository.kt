package app.flipday.data

import app.flipday.core.Event
import app.flipday.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val dao: EventDao, private val scheduler: ReminderScheduler) {
    fun observeEvents(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun all(): List<Event> = dao.getAll().map { it.toDomain() }

    suspend fun save(event: Event): Long {
        val id = dao.upsert(event.toEntity())
        scheduler.schedule(event.copy(id = id))
        return id
    }

    suspend fun delete(event: Event) {
        scheduler.cancel(event.id)
        dao.delete(event.toEntity())
    }

    /** Called after boot, time change or app update, when alarms are lost. */
    suspend fun rescheduleAll() = all().forEach { scheduler.schedule(it) }
}
