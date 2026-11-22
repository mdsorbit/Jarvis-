package com.mdsorbit.jarvis

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

class WakeWordService : Service() {
    private var porcupineManager: PorcupineManager? = null

    companion object {
        private const val CHANNEL_ID = "jarvis_wake_word"
        private const val NOTIFICATION_ID = 4101
        private const val TAG = "JarvisWakeService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Preparing offline wake-word detection"))

        if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) {
            updateNotification("AccessKey missing. Listening has not started.")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val callback = PorcupineManagerCallback { keywordIndex ->
                Log.i(TAG, "Wake word detected: index=$keywordIndex")
                updateNotification("Wake word detected! Open JARVIS to give a command.")
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setKeywords(arrayOf(Porcupine.BuiltInKeyword.PORCUPINE))
                .build(this, callback)

            porcupineManager?.start()
            updateNotification("Listening for “Porcupine” offline. Tap Stop in JARVIS to end.")
        } catch (error: PorcupineException) {
            Log.e(TAG, "Could not start Porcupine", error)
            updateNotification("Wake-word engine error. Check AccessKey and app setup.")
            stopSelf()
        } catch (error: Exception) {
            Log.e(TAG, "Unexpected wake-word error", error)
            updateNotification("Could not start listening.")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS microphone listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible status while JARVIS listens for its wake word"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS is active")
            .setContentText(message)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        try {
            porcupineManager?.stop()
        } catch (error: Exception) {
            Log.w(TAG, "Error stopping wake-word engine", error)
        }
        porcupineManager?.delete()
        porcupineManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
