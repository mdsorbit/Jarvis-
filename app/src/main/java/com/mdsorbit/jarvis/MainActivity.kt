package com.mdsorbit.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity(), RecognitionListener {
    private lateinit var status: TextView
    private lateinit var heard: TextView
    private lateinit var replyView: TextView
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var listening = false
    private var ttsReady = false
    private var retryCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val micRequest = 41
    private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val aiModel by lazy { Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-4-hydrogen") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        }
        buildUi()
        tts = TextToSpeech(this) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (ttsReady) {
                val lang = tts?.setLanguage(Locale("hi", "IN"))
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    replyView.text = "Hindi voice data is not available. Install Hindi voice in Android Text-to-speech settings."
                }
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { handler.post { if (listening) listenAgain(350) } }
                    override fun onError(id: String?) { handler.post { if (listening) listenAgain(700) } }
                })
            } else {
                replyView.text = "Text-to-speech service is unavailable."
            }
        }
    }

    private fun buildUi() {
        val navy = Color.rgb(7, 17, 37)
        val blue = Color.rgb(48, 126, 255)
        val pale = Color.rgb(205, 218, 238)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(26), dp(20), dp(22))
            setBackgroundColor(navy)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        fun label(value: String, size: Float, color: Int) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        content.addView(label("J.A.R.V.I.S", 32f, Color.WHITE), match(-1, 58))
        content.addView(label("PERSONAL AI • VOICE • PHONE • WEB", 11f, blue), match(-1, 34))
        status = label("SYSTEM READY", 18f, Color.WHITE)
        content.addView(status, match(-1, 55))
        heard = label("Aapki awaaz yahan dikhegi.", 16f, pale)
        content.addView(heard, match(-1, -2))
        replyView = label("Hindi voice assistant ready. START dabakar boliye.", 14f, pale)
        content.addView(replyView, match(-1, -2))
        val start = Button(this).apply {
            text = "START JARVIS"; setTextColor(Color.WHITE); setBackgroundColor(blue)
            setOnClickListener { requestAndStart() }
        }
        val stop = Button(this).apply { text = "STOP"; setOnClickListener { stopListening() } }
        val test = Button(this).apply { text = "TEST SPEAKER"; setOnClickListener { speak("नमस्ते, मैं जार्विस हूँ। आपकी आवाज़ सुनने के लिए तैयार हूँ।") } }
        content.addView(start, match(-1, 54))
        content.addView(stop, match(-1, 50))
        content.addView(test, match(-1, 50))
        content.addView(label(
            "Try: समय बताओ • तारीख बताओ • बैटरी कितनी है • Google पर खोजो [विषय] • YouTube खोलो • Chrome खोलो • Maps खोलो • [नाम] को कॉल करो • [नाम] को मैसेज भेजो [बात] • WhatsApp खोलो. Calls/messages send nahi honge; pehle phone confirmation screen khulegi.",
            12f, pale
        ), match(-1, -2))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun requestAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        } else startListening()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "SPEECH SERVICE MISSING"
            replyView.text = "Android speech recognition service enable/install karein. Google Speech Services update karein."
            return
        }
        if (recognizer == null) {
            // Use system recognizer, which can access online Hindi language services.
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)
        }
        listening = true
        retryCount = 0
        status.text = "LISTENING • HINDI"
        listenAgain(250)
    }

    private fun listenAgain(delay: Long) {
        if (!listening) return
        handler.postDelayed({
            if (!listening) return@postDelayed
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                status.text = "START ERROR"
                replyView.text = e.localizedMessage ?: "Speech start nahi ho saka."
                listening = false
            }
        }, delay)
    }

    private fun stopListening() {
        listening = false
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel() } catch (_: Exception) {}
        status.text = "STOPPED"
    }

    override fun onReadyForSpeech(params: Bundle?) { status.text = "I'M LISTENING…" }
    override fun onBeginningOfSpeech() { status.text = "HEARING YOU…" }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { status.text = "PROCESSING…" }

    override fun onError(error: Int) {
        if (!listening) return
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Speech service client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
            SpeechRecognizer.ERROR_NETWORK -> "Network error in speech service"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "Awaaz samajh nahi aayi. Thoda saaf aur paas se boliye."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz nahi mili. Dobara boliye."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Thoda wait karein."
            else -> "Speech error code: $error"
        }
        replyView.text = message
        status.text = "VOICE ERROR $error"
        // The system recognizer is already used; report errors and retry normally.
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
            error == SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) {
            listening = false
            return
        }
        retryCount++
        listenAgain(if (retryCount > 3) 1200 else 650)
    }

    override fun onResults(results: Bundle?) {
        val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!phrase.isNullOrBlank()) {
            heard.text = "YOU: $phrase"
            retryCount = 0
            handleCommand(phrase)
        } else if (listening) listenAgain(400)
    }

    override fun onPartialResults(results: Bundle?) {
        val p = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!p.isNullOrBlank()) heard.text = "HEARING: $p"
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun handleCommand(raw: String) {
        val q = raw.lowercase(Locale.ROOT).trim()
        val response: String
        when {
            q.contains("समय") || q.contains("टाइम") || q.contains("time") -> {
                response = "अभी समय है " + SimpleDateFormat("hh:mm a", Locale("hi", "IN")).format(Date())
            }
            q.contains("तारीख") || q.contains("दिनांक") || q.contains("date") || q.contains("आज कौन") -> {
                response = "आज " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())
            }
            q.contains("बैटरी") || q.contains("battery") -> {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                response = "आपके फोन की बैटरी " + bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + " प्रतिशत है।"
            }
            q.contains("google पर खोजो") || q.contains("गूगल पर खोजो") || q.startsWith("search ") || q.startsWith("खोजो ") -> {
                val term = raw.replace(Regex("(?i)google पर खोजो|गूगल पर खोजो|search|खोजो"), "").trim()
                if (term.isBlank()) { response = "क्या खोजना है, बताइए।"; speak(response); return }
                openWebSearch(term); response = "$term के लिए Google खोज खोल रहा हूँ।"
            }
            q.contains("youtube") || q.contains("यूट्यूब") || q.contains("यू ट्यूब") -> { openAppOrWeb("com.google.android.youtube", "https://www.youtube.com"); response = "YouTube खोल रहा हूँ।" }
            q.contains("chrome") || q.contains("क्रोम") -> { openAppOrWeb("com.android.chrome", "https://www.google.com"); response = "Chrome खोल रहा हूँ।" }
            q.contains("whatsapp") || q.contains("व्हाट्सएप") || q.contains("व्हाट्स ऐप") -> { openAppOrWeb("com.whatsapp", "https://www.whatsapp.com"); response = "WhatsApp खोल रहा हूँ।" }
            q.contains("maps") || q.contains("मैप") || q.contains("मैप्स") || q.contains("गूगल मैप") -> { openAppOrWeb("com.google.android.apps.maps", "https://maps.google.com"); response = "Maps खोल रहा हूँ।" }
            q.contains("कॉल") || q.contains("call ") -> {
                val who = raw.replace(Regex("(?i)कॉल करो|कॉल लगाओ|call me|call"), "").trim()
                if (who.isBlank()) { response = "किसे कॉल करना है? Phone app खोल रहा हूँ।"; startActivity(Intent(Intent.ACTION_DIAL)) }
                else { response = "$who के लिए dialer खोल रहा हूँ। नंबर चुनकर कॉल confirm karein."; startActivity(Intent(Intent.ACTION_DIAL).putExtra("android.intent.extra.PHONE_NUMBER", who)) }
            }
            q.contains("मैसेज") || q.contains("message") || q.contains("sms") -> {
                val body = raw.substringAfter("मैसेज भेजो", raw.substringAfter("message", "")).trim()
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", body)
                if (i.resolveActivity(packageManager) != null) startActivity(i)
                response = "Message app खोल रहा हूँ। भेजने से पहले आप confirm karein."
            }
            q.contains("सेटिंग") || q.contains("settings") -> { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); response = "Settings खोल रहा हूँ।" }
            q.contains("नमस्ते") || q.contains("हेलो") || q.contains("hello") || q.contains("jarvis") || q.contains("जार्विस") -> response = "जी, मैं सुन रहा हूँ। बताइए क्या करना है?"
            else -> { askGemini(raw); return }
        }
        speak(response)
    }

    private fun askGemini(question: String) {
        status.text = "JARVIS THINKING • GEMINI"
        replyView.text = "आपका सवाल समझ रहा हूँ…"
        aiScope.launch {
            try {
                val prompt = """
                    You are JARVIS, a helpful personal AI assistant for an Indian user.
                    Reply naturally in simple Hindi or Hinglish, matching the user's language.
                    Be accurate, concise, practical, and honest. If unsure, say so.
                    Do not claim to control phone features unless the app explicitly supports them.
                    User message: $question
                """.trimIndent()
                val result = withContext(Dispatchers.IO) {
                    aiModel.generateContent(prompt)
                }
                speak(result.text?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "माफ़ कीजिए, अभी जवाब नहीं मिल पाया।")
            } catch (e: Exception) {
                val detail = e.localizedMessage?.take(180) ?: "Unknown error"
                speak("Gemini से कनेक्शन नहीं हो पाया। इंटरनेट और Firebase App Check सेटिंग जाँचें। विवरण: $detail")
            }
        }
    }

    private fun openWebSearch(term: String) {
        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", term)
        try { startActivity(i) } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(term))))
        }
    }

    private fun openAppOrWeb(pkg: String, url: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        try { startActivity(launch ?: Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun speak(text: String) {
        replyView.text = text
        status.text = "JARVIS RESPONDING"
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        else if (listening) listenAgain(900)
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (code == micRequest) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
            else { status.text = "MIC PERMISSION DENIED"; replyView.text = "Settings mein JARVIS ko microphone permission dein." }
        }
    }

    override fun onDestroy() {
        listening = false
        aiScope.cancel()
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun match(w: Int, h: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(if (w < 0) w else dp(w), if (h < 0) h else dp(h))
}