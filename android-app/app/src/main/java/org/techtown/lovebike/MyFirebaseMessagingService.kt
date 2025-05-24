package org.techtown.lovebike

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "📨 메시지 수신: ${remoteMessage.data}")
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.data["title"] ?: "도난 경고"
        val body = remoteMessage.data["body"] ?: "등록되지 않은 사용자가 감지되었습니다."

        val videoUrl = remoteMessage.data["video_url"] ?: ""
        val intent = Intent(this, WatchVideoActivity::class.java).apply {
            putExtra("video_url", videoUrl)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "alert_channel"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "도난 경고", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        manager.notify(0, builder.build())
    }
}
