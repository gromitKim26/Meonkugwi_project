package org.techtown.lovebike.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.POST

interface FlaskApi {
    @POST("/start-camera")
    fun startCamera(): Call<ResponseBody>
}