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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                            onDismiss = {
                                stopListening()
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
            
            // Set speech accent/locale
            val currentLocale = when (prefs.ttsLocale) {
                "UK" -> Locale.UK
                "CA" -> Locale.CANADA
                "AU" -> Locale("en", "AU")
                "IN" -> Locale("en", "IN")
                else -> Locale.US
            }
            textToSpeech?.setLanguage(currentLocale)

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

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningState.value = true
                        transcribedTextState.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        transcribedTextState.value = "Listening..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        transcribedTextState.value = "Processing..."
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                            else -> "Speech recognition error"
                        }
                        Log.e(TAG, "Speech Error: $message ($error)")
                        transcribedTextState.value = "Error: $message"
                        // Close listening overlay shortly after error
                        isListeningState.value = false
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
            speechRecognizer?.stopListening()
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("listening_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Speaking Practice",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Pulsing-like indicator or loading spinner
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )

                Text(
                    text = transcribedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Stop Listening")
                }
            }
        }
    }
}
