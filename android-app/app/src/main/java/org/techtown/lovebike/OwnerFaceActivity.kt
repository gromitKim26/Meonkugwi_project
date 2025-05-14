package org.techtown.lovebike

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.ResponseBody
import org.techtown.lovebike.network.FlaskApi
import org.techtown.lovebike.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerFaceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_owner_face)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ 여기서 서버로 얼굴 등록 요청 보내기
        startOwnerFaceRegistration()
    }

    private fun startOwnerFaceRegistration() {
        val flaskApi = RetrofitClient.instance.create(FlaskApi::class.java)
        flaskApi.startCamera().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Toast.makeText(this@OwnerFaceActivity, "얼굴 등록 완료!", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@OwnerFaceActivity, "서버 연결 실패: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
