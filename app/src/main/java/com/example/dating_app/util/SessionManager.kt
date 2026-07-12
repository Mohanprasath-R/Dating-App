package com.example.dating_app.util

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

object SessionManager {
    private var logoutHandler: Handler? = null
    private const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes

    private val logoutRunnable = Runnable {
        logout()
    }

    fun startSessionTimer() {
        resetSessionTimer()
    }

    fun resetSessionTimer() {
        stopSessionTimer()
        logoutHandler = Handler(Looper.getMainLooper())
        logoutHandler?.postDelayed(logoutRunnable, SESSION_TIMEOUT)
    }

    fun stopSessionTimer() {
        logoutHandler?.removeCallbacks(logoutRunnable)
        logoutHandler = null
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        // In a real app, broadcast an event to finish all activities and return to Login
    }
}
