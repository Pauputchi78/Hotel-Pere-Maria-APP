package com.example.hotel_pere_maria_app.ui.Service

import com.example.hotel_pere_maria_app.ui.Models.BookingAudit
import retrofit2.Response
import java.util.List
import retrofit2.http.GET
import retrofit2.http.Path

interface BookingauditService {
    @GET("bookings/{reservation_id}/audit")
    suspend fun getReservationLogs(@Path("reservation_id") reservation_id: String): Response<List<BookingAudit>>
}