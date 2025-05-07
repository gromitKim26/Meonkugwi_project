package org.techtown.lovebike

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.OnMapReadyCallback
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import com.google.android.gms.location.Priority
import android.location.Location
import org.techtown.lovebike.data.RideRecord
import org.techtown.lovebike.data.AppDatabase
import androidx.room.Room
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch




class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var pathPoints = mutableListOf<LatLng>()
    private lateinit var timerTextView: TextView
    private var timerStarted = false
    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var speedTextView: TextView
    private var totalSpeed = 0.0
    private var locationCount = 0

    private var totalDistance = 0.0  // 총 이동 거리 (미터 단위)
    private var lastLocation: Location? = null  // 이전 위치 저장용
    private lateinit var distanceTextView: TextView

    private fun startRide() {
        if (!timerStarted) {
            timerStarted = true
            startTime = System.currentTimeMillis()
            handler.post(timerRunnable)

            // 위치 추적 시작
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                    .setMinUpdateIntervalMillis(2000)
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRide() {
        if (timerStarted) {
            timerStarted = false
            handler.removeCallbacks(timerRunnable)
            saveRideRecord() // 이걸 여기서 호출

                        // 위치 추적 중단
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun saveRideRecord() {
        val record = RideRecord(
            date = getTodayDate(), // 오늘 날짜
            time = timerTextView.text.toString(), // 타이머에 표시된 시간
            distance = totalDistance.toFloat(),   // 누적 거리
            avgSpeed = totalSpeed.toFloat() / locationCount  // 평균 속도
        )

        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "ride_database"
            ).build()

            db.rideRecordDao().insert(record)
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }



    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 1000) / 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        timerTextView = findViewById(R.id.text_timer)
        speedTextView = findViewById(R.id.text_speed)
        distanceTextView = findViewById(R.id.text_distance)



        val btnStartRide = findViewById<Button>(R.id.btn_start_ride)
        val btnStopRide = findViewById<Button>(R.id.btn_stop_ride)

        btnStartRide.setOnClickListener {
            startRide() //  타이머 시작
        }

        btnStopRide.setOnClickListener {
            stopRide() //  타이머 정지
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 위치 권한 확인 후 내 위치 표시
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
            return
        }

        map.isMyLocationEnabled = true // 내 위치 버튼 표시 (파란 점)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5초마다 위치 업데이트 요청
            fastestInterval = 2000 // 최소 2초 간격
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                for (location in locationResult.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    pathPoints.add(latLng)

                    // 거리 계산
                    lastLocation?.let { previous ->
                        totalDistance += previous.distanceTo(location) // m 단위
                    }
                    lastLocation = location

                    // 속도 계산
                    val speedInMeterPerSecond = location.speed
                    val averageSpeed = if (totalDistance > 0 && timerStarted) {
                        val elapsedTimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                        totalDistance / elapsedTimeSeconds
                    } else {
                        0.0
                    }

                    // UI 업데이트
                    speedTextView.text = String.format("%.1f m/s (%.1f m/s)", speedInMeterPerSecond, averageSpeed)
                    distanceTextView.text = String.format("%.1f m", totalDistance)
                }


                for (location in locationResult.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    pathPoints.add(latLng)

                    for (location in locationResult.locations) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        pathPoints.add(latLng)

                        // 속도 계산 및 표시
                        val speedInMeterPerSecond = location.speed  // m/s
                        totalSpeed += speedInMeterPerSecond
                        locationCount++

                        val averageSpeed = if (locationCount > 0) totalSpeed / locationCount else 0.0
                        speedTextView.text = String.format("%.1f m/s (%.1f m/s)", speedInMeterPerSecond, averageSpeed)
                    }

                    // 경로 그리기
                    map.addPolyline(
                        PolylineOptions()
                            .color(Color.BLUE)
                            .width(10f)
                            .addAll(pathPoints)
                    )

                    // 카메라 이동 (선택)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1000 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 확인 후 실행
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
            }
        }
    }
}


