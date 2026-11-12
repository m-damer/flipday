package app.flipday

import android.app.Application
import app.flipday.core.Event
import app.flipday.data.EventRepository
import app.flipday.data.FlipdayDatabase
import app.flipday.data.SettingsStore
import app.flipday.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow

/** Manual dependency container. Small enough that a DI framework would add more than it removes. */
class AppContainer(app: Application) {
    private val db = FlipdayDatabase.build(app)
    val settings = SettingsStore(app)
    val scheduler = ReminderScheduler(app)
    val repository = EventRepository(db.eventDao(), scheduler)

    /** Last deleted event, kept so the Events screen can offer Undo after a delete from the detail screen. */
    val pendingUndo = MutableStateFlow<Event?>(null)
}

class FlipdayApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scheduler.ensureChannel()
    }
}
