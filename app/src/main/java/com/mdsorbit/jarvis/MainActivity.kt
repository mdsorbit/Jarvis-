package com.mdsorbit.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {
    private lateinit var statusText: TextView
    private lateinit var heardText: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var listening = false
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private val requestMicCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        textToSpeech = TextToSpeech(this) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale("hi", "IN")
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        handler.post { if (listening) launchRecognition() }
                    }
                    override fun onError(utteranceId: String?) {
                        handler.post { if (listening) launchRecognition() }
                    }
                })
            }
        }
    }

    private fun buildScreen() {
        val navy = Color.rgb(8, 18, 38)
        val blue = Color.rgb(52, 130, 255)
        val white = Color.WHITE
        val muted = Color.rgb(176, 190, 212)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(32), dp(20), dp(24))
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
            setPadding(0, dp(24), 0, dp(8))
        }
        heardText = TextView(this).apply {
            text = "Your recognized speech will appear here."
            textSize = 15f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(16), dp(8), dp(20))
        }
        val info = TextView(this).apply {
            text = "No Picovoice account or API key needed. Tap START and speak in Hindi. On supported Android phones, JARVIS uses the on-device speech recognizer; otherwise Android may use its configured speech service. This starter version does not listen for the custom wake word while the screen is locked."
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setPadding(dp(8), dp(8), dp(8), dp(20))
        }
        val startButton = Button(this).apply {
            text = "START VOICE ASSISTANT"
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener { requestAndStart() }
        }
        val stopButton = Button(this).apply {
            text = "STOP LISTENING"
            setOnClickListener { stopListening() }
        }

        root.addView(title, match(ViewGroup.LayoutParams.MATCH_PARENT, 58))
        root.addView(subtitle, match(ViewGroup.LayoutParams.MATCH_PARENT, 30))
        root.addView(statusText, match(ViewGroup.LayoutParams.MATCH_PARENT, 58))
        root.addView(heardText, match(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(info, match(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(startButton, match(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        root.addView(stopButton, match(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        setContentView(root)
    }

    private fun requestAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMicCode)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "SPEECH SERVICE NOT AVAILABLE"
            Toast.makeText(this, "Install or enable a speech recognition service on Android.", Toast.LENGTH_LONG).show()
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = if (Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }.also { it.setRecognitionListener(this) }
        }

        listening = true
        statusText.text = if (Build.VERSION.SDK_INT >= 31 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) "LISTENING • ON-DEVICE" else "LISTENING • ANDROID SPEECH SERVICE"
        launchRecognition()
    }

    private fun launchRecognition() {
        if (!listening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (error: Exception) {
            statusText.text = "COULD NOT START SPEECH"
            listening = false
        }
    }

    private fun stopListening() {
        listening = false
        handler.removeCallbacksAndMessages(null)
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
        statusText.text = "LISTENING STOPPED"
    }

    override fun onReadyForSpeech(params: Bundle?) { statusText.text = "I'M LISTENING…" }
    override fun onBeginningOfSpeech() { statusText.text = "HEARING YOUR VOICE…" }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { statusText.text = "PROCESSING…" }

    override fun onError(error: Int) {
        if (!listening) return
        statusText.text = "LISTENING…"
        handler.postDelayed({ launchRecognition() }, 700)
    }

    override fun onResults(results: Bundle?) {
        val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!phrase.isNullOrBlank()) {
            heardText.text = "You said: $phrase"
            val reply = when {
                phrase.contains("समय") || phrase.contains("time", true) -> "Maaf kijiye, abhi clock feature connect nahi hai."
                phrase.contains("नमस्ते") || phrase.contains("हेलो") || phrase.contains("hello", true) ||
                    phrase.contains("जार्विस") || phrase.contains("jarvis", true) -> "जी, मैं सुन रहा हूँ।"
                else -> "आपने कहा, $phrase"
            }
            statusText.text = "JARVIS RESPONDING"
            if (ttsReady) {
                textToSpeech?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
            } else {
                handler.postDelayed({ launchRecognition() }, 900)
            }
        } else if (listening) {
            handler.postDelayed({ launchRecognition() }, 500)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val phrase = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!phrase.isNullOrBlank()) heardText.text = phrase
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestMicCode) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                statusText.text = "MICROPHONE PERMISSION NEEDED"
            }
        }
    }

    override fun onDestroy() {
        listening = false
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun match(width: Int, height: Int): LinearLayout.LayoutParams {
        val layoutWidth = if (width < 0) width else dp(width)
        val layoutHeight = if (height < 0) height else dp(height)
        return LinearLayout.LayoutParams(layoutWidth, layoutHeight)
    }
}