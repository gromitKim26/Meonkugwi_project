package org.techtown.lovebike.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_records")
data class RideRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val distance: Float,   // meters
    val avgSpeed: Float    // m/s
)


