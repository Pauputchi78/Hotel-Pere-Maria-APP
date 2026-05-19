package com.example.hotel_pere_maria_app.ui.Models

import java.util.Date

data class BookingAudit(
    val _id: String,
    val reservation_id: String,
    val action: String,
    val user_id: String,
    val details: String,
    val timestamp: Date
)
