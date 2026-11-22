package com.mdsorbit.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private val requestPermissionsCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val navy = Color.rgb(8, 18, 38)
        val blue = Color.rgb(52, 130, 255)
        val white = Color.WHITE
        val muted = Color.rgb(176, 190, 212)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 32)
            setBackgroundColor(navy)
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 30f
            setTextColor(white)
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }

        val subtitle = TextView(this).apply {
            text = "PERSONAL VOICE ASSISTANT"
            textSize = 11f
            setTextColor(blue)
            gravity = Gravity.CENTER
            letterSpacing = 0.16f
        }

        statusText = TextView(this).apply {
            text = "SYSTEM READY"
            textSize = 18f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 42, 0, 12)
        }

        val info = TextView(this).apply {
            text = "Offline wake-word engine: Porcupine\nWake word: “Jarvis”\n\nPress START and grant microphone permission. Android will show a persistent notification and microphone indicator while listening.\n\nAfter wake-word detection, Hindi speech recognition and spoken replies will be added in the next stage."
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setLineSpacing(6f, 1f)
            setPadding(8, 12, 8, 24)
        }

        val startButton = Button(this).apply {
            text = "START LISTENING"
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener { requestAndStart() }
        }

        val stopButton = Button(this).apply {
            text = "STOP LISTENING"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WakeWordService::class.java))
                statusText.text = "LISTENING STOPPED"
            }
        }

        root.addView(title, match(ViewGroup.LayoutParams.MATCH_PARENT, 58))
        root.addView(subtitle, match(ViewGroup.LayoutParams.MATCH_PARENT, 30))
        root.addView(statusText, match(ViewGroup.LayoutParams.MATCH_PARENT, 64))
        root.addView(info, match(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(startButton, match(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        root.addView(stopButton, match(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        setContentView(root)
    }

    private fun requestAndStart() {
        if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) {
            Toast.makeText(this, "Picovoice AccessKey is not configured. Follow the README setup.", Toast.LENGTH_LONG).show()
            statusText.text = "ACCESS KEY REQUIRED"
            return
        }

        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), requestPermissionsCode)
        } else {
            startWakeWordService()
        }
    }

    private fun startWakeWordService() {
        val intent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "STARTING JARVIS WAKE WORD..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionsCode) {
            val microphoneGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (microphoneGranted) {
                startWakeWordService()
            } else {
                statusText.text = "MICROPHONE PERMISSION NEEDED"
                Toast.makeText(this, "Microphone permission is required to listen.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun match(width: Int, height: Int) = LinearLayout.LayoutParams(width, height)
}
