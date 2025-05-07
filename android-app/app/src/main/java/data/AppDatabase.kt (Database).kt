package org.techtown.lovebike.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RideRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideRecordDao(): RideRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ride_record_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
