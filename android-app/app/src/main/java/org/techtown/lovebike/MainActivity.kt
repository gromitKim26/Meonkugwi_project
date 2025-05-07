package org.techtown.lovebike

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartRide = findViewById<Button>(R.id.btn_start_ride)
        val btnViewHistory = findViewById<Button>(R.id.btn_view_history)

        btnStartRide.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            startActivity(intent)
        }

        btnViewHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }
}
