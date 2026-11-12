package app.flipday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventEntity::class], version = 1, exportSchema = true)
abstract class FlipdayDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        fun build(context: Context): FlipdayDatabase =
            Room.databaseBuilder(context, FlipdayDatabase::class.java, "flipday.db").build()
    }
}
