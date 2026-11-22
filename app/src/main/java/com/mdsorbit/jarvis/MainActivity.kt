package com.mdsorbit.jarvis

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navy = Color.rgb(8, 18, 38)
        val blue = Color.rgb(52, 130, 255)
        val white = Color.WHITE
        val muted = Color.rgb(176, 190, 212)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 56, 28, 32)
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
            setPadding(0, 56, 0, 12)
        }

        val info = TextView(this).apply {
            text = "JARVIS project foundation is installed.\n\nNext modules: microphone permission, wake-word detection, Hindi speech recognition, voice replies and optional online AI.\n\nAlways-listening mode is not active in this starter build."
            textSize = 15f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setLineSpacing(7f, 1f)
            setPadding(8, 12, 8, 28)
        }

        val button = Button(this).apply {
            text = "CHECK PROJECT STATUS"
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener {
                statusText.text = "STARTER BUILD READY"
            }
        }

        root.addView(title, match(ViewGroup.LayoutParams.MATCH_PARENT, 60))
        root.addView(subtitle, match(ViewGroup.LayoutParams.MATCH_PARENT, 32))
        root.addView(statusText, match(ViewGroup.LayoutParams.MATCH_PARENT, 70))
        root.addView(info, match(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(button, match(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        setContentView(root)
    }

    private fun match(width: Int, height: Int) =
        LinearLayout.LayoutParams(width, height)
}
