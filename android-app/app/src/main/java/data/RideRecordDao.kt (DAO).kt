package org.techtown.lovebike.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RideRecordDao {
    @Insert
    suspend fun insert(record: RideRecord)

    @Query("SELECT * FROM ride_records ORDER BY id DESC")
    suspend fun getAllRecords(): List<RideRecord>

    @Query("SELECT * FROM ride_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: String): RideRecord?

    @Query("SELECT * FROM ride_records WHERE date = :selectedDate")
    fun getRecordsByDate(selectedDate: String): List<RideRecord>

}
