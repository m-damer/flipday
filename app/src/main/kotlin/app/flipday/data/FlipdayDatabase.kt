package app.flipday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventEntity::class], version = 2, exportSchema = true)
abstract class FlipdayDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        fun build(context: Context): FlipdayDatabase =
            Room.databaseBuilder(context, FlipdayDatabase::class.java, "flipday.db").addMigrations(MIGRATION_1_2).build()

        /** Version 2 adds the nullable end day used by periods. Existing events keep their data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN endEpochDay INTEGER")
            }
        }
    }
}
