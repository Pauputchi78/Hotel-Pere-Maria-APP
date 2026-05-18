package com.example.hotel_pere_maria_app.ui.Service

import com.example.hotel_pere_maria_app.ui.Models.Reservation
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ReservationService{
    @GET("reservation/mine")
    suspend fun getMine(): Response<List<Reservation>>

    @POST("reservation/getPrice")
    suspend fun getPrice(@Body datos : Map<String, String>): Response<Map<String,Double>>

    @POST("reservation/add")
    suspend fun addReservation(@Body datos: Map<String, String>): Response<Map<String, String>>

    @POST("reservation/cancel")
    suspend fun cancelReservation(@Body datos: Map<String, String>):Response<Map<String, Any>>

    @POST("reservation/getCancelationPrice")
    suspend fun cancelationPrice(@Body datos: Map<String, String>): Response<Map<String, String>>

    @PUT("reservation/update")
    suspend fun updateReservation(@Body datos : Map<String, String>): Response<Map<String, Any>>

    @Streaming
    @GET("reservation/{reservation_id}/invoice")
    suspend fun descargarFacturaPdf(@Path("reservation_id") reservation_id: String): Response<ResponseBody>
}