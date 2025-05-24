package org.techtown.lovebike

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.techtown.lovebike.OwnerFaceActivity
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registerBtn = findViewById<Button>(R.id.registerOwnerBtn)
        registerBtn.setOnClickListener {
            val intent = Intent(this, OwnerFaceActivity::class.java)
            startActivity(intent)
        }

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

        FirebaseMessaging.getInstance().subscribeToTopic("alert")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "🚨 도난 알림 토픽 구독 완료")
                } else {
                    Log.e("FCM", "❌ 도난 알림 토픽 구독 실패")
                }
            }

    }
}
