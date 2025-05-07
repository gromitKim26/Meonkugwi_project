package org.techtown.lovebike

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import org.techtown.lovebike.data.AppDatabase
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {

    private lateinit var timeTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var dateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 뷰 연결
        dateButton = findViewById(R.id.btn_select_date)
        timeTextView = findViewById(R.id.text_time)
        distanceTextView = findViewById(R.id.text_distance)
        avgSpeedTextView = findViewById(R.id.text_avg_speed)

        // 버튼 클릭 시 날짜 선택 다이얼로그
        dateButton.setOnClickListener {
            showDatePicker()
        }

        // Room DB 불러와서 기록 표시 (비동기 처리 필수)
        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                name = "ride_database"
            ).build()

            val records = db.rideRecordDao().getAllRecords()

            if (records.isNotEmpty()) {
                val lastRecord = records.last()
                timeTextView.text = lastRecord.time
                distanceTextView.text = "${lastRecord.distance} m"
                avgSpeedTextView.text = "${lastRecord.avgSpeed} m/s"
            } else {
                timeTextView.text = "기록 없음"
                distanceTextView.text = "-"
                avgSpeedTextView.text = "-"
            }
        }
    }

    private fun showRideRecordForDate(selectedDate: String) {
        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                name = "ride_database"
            ).build()

            val records = db.rideRecordDao().getRecordsByDate(selectedDate)

            if (records.isNotEmpty()) {
                val record = records.last()
                timeTextView.text = record.time
                distanceTextView.text = "${record.distance} m"
                avgSpeedTextView.text = "${record.avgSpeed} m/s"
            } else {
                timeTextView.text = "해당 날짜에 기록이 없습니다"
                distanceTextView.text = "-"
                avgSpeedTextView.text = "-"
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(this, { _, y, m, d ->
            val selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            showRideRecordForDate(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}
