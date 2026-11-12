package app.flipday.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.core.ReminderRule
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val epochDay: Long,
    val minuteOfDay: Int?,
    val startEpochDay: Long?,
    /** "daysBefore@minuteOfDay" pairs separated by commas, e.g. "7@540,1@540". */
    val reminders: String,
    val repeatYearly: Boolean,
    val archivedEpochDay: Long?,
    val milestoneEveryDays: Int?,
)

fun EventEntity.toDomain() = Event(
    id = id, title = title, type = EventType.valueOf(type), date = LocalDate.ofEpochDay(epochDay),
    time = minuteOfDay?.let { LocalTime.ofSecondOfDay(it * 60L) },
    startDate = startEpochDay?.let(LocalDate::ofEpochDay),
    reminders = reminders.split(',').filter { it.isNotBlank() }.map {
        val (d, m) = it.split('@'); ReminderRule(d.toInt(), LocalTime.ofSecondOfDay(m.toInt() * 60L))
    },
    repeatYearly = repeatYearly, archivedOn = archivedEpochDay?.let(LocalDate::ofEpochDay), milestoneEveryDays = milestoneEveryDays,
)

fun Event.toEntity() = EventEntity(
    id = id, title = title, type = type.name, epochDay = date.toEpochDay(),
    minuteOfDay = time?.let { it.hour * 60 + it.minute }, startEpochDay = startDate?.toEpochDay(),
    reminders = reminders.joinToString(",") { "${it.daysBefore}@${it.at.hour * 60 + it.at.minute}" },
    repeatYearly = repeatYearly, archivedEpochDay = archivedOn?.toEpochDay(), milestoneEveryDays = milestoneEveryDays,
)
