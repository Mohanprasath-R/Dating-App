package com.example.dating_app.util

import java.util.Calendar

object DateUtils {
    fun getAgeFromDob(dob: String): Int {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val birthYear = dob.split("/").lastOrNull()?.toIntOrNull() ?: 2000
            currentYear - birthYear
        } catch (e: Exception) {
            24
        }
    }
}
