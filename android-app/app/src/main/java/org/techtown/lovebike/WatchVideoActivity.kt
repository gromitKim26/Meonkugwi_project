package org.techtown.lovebike

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class WatchVideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_video)

        val videoUrl = intent.getStringExtra("video_url")

        if (videoUrl != null) {
            // 알림으로 영상 바로 왔을 때는 자동 재생
            playVideo(videoUrl)
        } else {
            // 알림 없으면 영상 목록 자동 띄우기
            fetchVideoListAndShowDialog()
        }
    }

    private fun fetchVideoListAndShowDialog() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.8.234:5000/videos/list")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WatchVideoActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                val videoList = JSONArray(json)
                val titles = Array(videoList.length()) { i -> videoList.getString(i) }

                runOnUiThread {
                    AlertDialog.Builder(this@WatchVideoActivity)
                        .setTitle("도난 영상 선택")
                        .setItems(titles) { _, which ->
                            val selectedUrl = titles[which]
                            playVideo(selectedUrl)
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        })
    }

    private fun playVideo(url: String) {
        val videoView = findViewById<VideoView>(R.id.videoView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(Uri.parse(url))
        videoView.setOnPreparedListener {
            videoView.start()
        }
    }
}
