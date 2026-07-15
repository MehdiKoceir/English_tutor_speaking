package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TutorViewModel
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val TAG = "MainActivity"

    private val prefs by lazy { com.example.data.PreferencesHelper(this) }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // State to manage the listening status overlay in Compose
    private val isListeningState = mutableStateOf(false)
    private val transcribedTextState = mutableStateOf("")
    private val rmsDbState = mutableStateOf(0f)
    private var onSpeechResultCallback: ((String) -> Unit)? = null

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechInputInternal()
        } else {
            Toast.makeText(
                this,
                "Microphone permission is required for voice-to-text English practice.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Text to Speech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            MyApplicationTheme {
                val viewModel: TutorViewModel = viewModel()
                
                LaunchedEffect(isTtsInitialized) {
                    if (isTtsInitialized) {
                        try {
                            val allVoices = textToSpeech?.voices ?: emptySet()
                            val engVoices = allVoices.filter {
                                it.locale != null && it.locale.language.startsWith("en", ignoreCase = true)
                            }.map { it.name }.sorted()
                            viewModel.updateAvailableVoices(engVoices)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error retrieving available TTS voices", e)
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppNavigation(
                        viewModel = viewModel,
                        onStartSpeechRecognizer = { onResult ->
                            onSpeechResultCallback = onResult
                            checkPermissionAndStartSpeech()
                        },
                        onSpeak = { text ->
                            speakText(text)
                        },
                        onStopSpeaking = {
                            stopSpeaking()
                        }
                    )

                    // Listening Dialog Overlay
                    if (isListeningState.value) {
                        ListeningDialog(
                            transcribedText = transcribedTextState.value,
                            rmsDb = rmsDbState.value,
                            onDismiss = {
                                stopListening()
                            },
                            onRetry = {
                                checkPermissionAndStartSpeech()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Text-to-Speech (TTS) Implementation ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "English language not supported for TTS")
            } else {
                isTtsInitialized = true
            }
        } else {
            Log.e(TAG, "Initialization of TTS failed")
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) {
            // Stop any ongoing speech first
            textToSpeech?.stop()
            
            // Set speech rate speed
            val rate = prefs.ttsRate
            textToSpeech?.setSpeechRate(rate)

            // Set speech pitch
            val pitch = prefs.ttsPitch
            textToSpeech?.setPitch(pitch)
            
            // Set speech accent/locale
            val currentLocale = when (prefs.ttsLocale) {
                "UK" -> Locale.UK
                "CA" -> Locale.CANADA
                "AU" -> Locale("en", "AU")
                "IN" -> Locale("en", "IN")
                else -> Locale.US
            }
            textToSpeech?.setLanguage(currentLocale)

            // Set custom voice if selected
            val voiceName = prefs.ttsVoiceName
            if (!voiceName.isNullOrEmpty()) {
                try {
                    val foundVoice = textToSpeech?.voices?.find { it.name == voiceName }
                    if (foundVoice != null) {
                        textToSpeech?.voice = foundVoice
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting custom voice $voiceName", e)
                }
            }

            // Clean up text if it contains errors or tags
            val cleanText = text.replace(Regex("\\[Error:[^\\]]*\\]"), "")
            if (cleanText.trim().isNotEmpty()) {
                textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "TUTOR_SPEECH_ID")
            }
        } else {
            Log.w(TAG, "TTS not initialized yet")
        }
    }

    private fun stopSpeaking() {
        if (isTtsInitialized) {
            textToSpeech?.stop()
        }
    }

    // --- Speech-to-Text (STT) Implementation ---
    private fun checkPermissionAndStartSpeech() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechInputInternal()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startSpeechInputInternal() {
        runOnUiThread {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                }

                // Reset state variables
                isListeningState.value = true
                transcribedTextState.value = "Listening..."
                rmsDbState.value = 0f

                // Dynamically fetch target practice language accent
                val currentLanguage = when (prefs.ttsLocale) {
                    "UK" -> "en-GB"
                    "CA" -> "en-CA"
                    "AU" -> "en-AU"
                    "IN" -> "en-IN"
                    else -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    // Robust timing configurations for language learners
                    putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 3000L)
                    putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
                    putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningState.value = true
                        transcribedTextState.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        transcribedTextState.value = "Listening..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Map live DB levels (typically -2dB to 10dB+) to 0.01f to 1.0f range
                        val normalized = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.01f, 1.0f)
                        rmsDbState.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        transcribedTextState.value = "Processing..."
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check your microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error. Ensure Speech Services by Google are enabled."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timeout."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak closer to the microphone."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Server processing error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
                            else -> "Speech recognition failed. Please try again."
                        }
                        Log.e(TAG, "Speech Error: $message ($error)")
                        transcribedTextState.value = "Error: $message"
                        // Keep dialog open so user can see error and click Retry
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val resultText = matches[0]
                            transcribedTextState.value = resultText
                            onSpeechResultCallback?.invoke(resultText)
                        }
                        isListeningState.value = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            transcribedTextState.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognizer", e)
                Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
                isListeningState.value = false
            }
        }
    }

    private fun stopListening() {
        runOnUiThread {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/cancelling speech recognizer", e)
            }
            isListeningState.value = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}

@Composable
fun ListeningDialog(
    transcribedText: String,
    rmsDb: Float,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val isError = transcribedText.startsWith("Error:")
    val isProcessing = transcribedText == "Processing..."
    val isListening = !isError && !isProcessing

    // Dynamic scale for pulsing effect based on live microphone volume
    val pulseScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isListening) 1.0f + (rmsDb * 0.45f) else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "PulseScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("listening_dialog")
                .border(
                    BorderStroke(
                        1.dp,
                        if (isError) Color(0x33EF4444) else Color(0x1A8B5CF6)
                    ),
                    RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isError) Color(0xFFFFF5F5) else Color(0xFFFAFAFE)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when {
                        isError -> Color(0xFFEF4444)
                        isProcessing -> Color(0xFF4F46E5)
                        else -> Color(0xFF8B5CF6)
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Text(
                        text = when {
                            isError -> "Practice Paused"
                            isProcessing -> "Transcribing..."
                            else -> "Speak Now"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Visual Area
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        // Outer halo pulsing ring
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0x1A8B5CF6))
                        )
                        // Inner ring
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(1.0f + (rmsDb * 0.2f))
                                .clip(CircleShape)
                                .background(Color(0x268B5CF6))
                        )
                        // Mic button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                                contentDescription = "Listening",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color(0xFF4F46E5),
                            strokeWidth = 4.dp
                        )
                    } else {
                        // Error visual
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Audio wave visualization (live equalizer bars)
                if (isListening) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(24.dp)
                    ) {
                        // Create 5 dynamic animated-like bars responding directly to the microphone volume
                        val heights = listOf(0.4f, 0.8f, 1.0f, 0.6f, 0.3f)
                        heights.forEach { factor ->
                            val barHeight = (4.dp + (20 * rmsDb * factor).dp).coerceIn(4.dp, 24.dp)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF8B5CF6))
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Transcribed / Prompt Text
                val textStyle = if (isError) {
                    MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFEF4444))
                } else {
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isProcessing) FontWeight.Normal else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val displayText = if (isError) {
                    transcribedText.removePrefix("Error:")
                } else {
                    transcribedText
                }

                Text(
                    text = displayText,
                    style = textStyle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action controls
                if (isError) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Close")
                        }

                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProcessing) Color(0xFF4F46E5) else Color(0xFF8B5CF6),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isProcessing) androidx.compose.material.icons.Icons.Default.Close else androidx.compose.material.icons.Icons.Default.Mic,
                            contentDescription = "Stop icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isProcessing) "Cancel" else "Stop Listening")
                    }
                }
            }
        }
    }
}
