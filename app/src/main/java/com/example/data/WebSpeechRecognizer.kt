package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class WebSpeechRecognizer(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onSpeechStart: () -> Unit,
    private val onSpeechEnd: () -> Unit,
    private val onResult: (String, Boolean) -> Unit, // text, isFinal
    private val onError: (String) -> Unit
) {
    private val TAG = "WebSpeechRecognizer"
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isInitialized = false

    init {
        mainHandler.post {
            try {
                Log.d(TAG, "Initializing background WebView for Web Speech API speech-to-text")
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun postSpeechStart() {
                            Log.d(TAG, "WebSpeech: postSpeechStart called from JS")
                            mainHandler.post { onSpeechStart() }
                        }

                        @JavascriptInterface
                        fun postSpeechEnd() {
                            Log.d(TAG, "WebSpeech: postSpeechEnd called from JS")
                            mainHandler.post { onSpeechEnd() }
                        }

                        @JavascriptInterface
                        fun postResult(text: String, isFinal: Boolean) {
                            Log.d(TAG, "WebSpeech: postResult called from JS: \"$text\" (isFinal=$isFinal)")
                            mainHandler.post { onResult(text, isFinal) }
                        }

                        @JavascriptInterface
                        fun postError(error: String) {
                            Log.e(TAG, "WebSpeech: postError called from JS: $error")
                            mainHandler.post { onError(error) }
                        }
                    }, "SpeechBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            Log.d(TAG, "WebSpeech: WebChromeClient.onPermissionRequest granting audio record permissions")
                            // Grant microphone record permission to WebView Web Speech API
                            request.grant(request.resources)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d(TAG, "WebSpeech: Background WebView finished loading HTML engine")
                            isInitialized = true
                            onReady()
                        }
                    }
                }

                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                    </head>
                    <body>
                        <script>
                            var recognition;
                            var isListening = false;
                            
                            function startRecognition(lang) {
                                try {
                                    if (recognition) {
                                        recognition.abort();
                                    }
                                    
                                    var SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                                    if (!SpeechRecognition) {
                                        SpeechBridge.postError("Web Speech API is not supported in this WebView.");
                                        return;
                                    }
                                    
                                    recognition = new SpeechRecognition();
                                    recognition.continuous = true;
                                    recognition.interimResults = true;
                                    recognition.lang = lang || 'en-US';
                                    
                                    recognition.onstart = function() {
                                        isListening = true;
                                        SpeechBridge.postSpeechStart();
                                    };
                                    
                                    recognition.onerror = function(event) {
                                        SpeechBridge.postError(event.error);
                                    };
                                    
                                    recognition.onend = function() {
                                        isListening = false;
                                        SpeechBridge.postSpeechEnd();
                                    };
                                    
                                    recognition.onresult = function(event) {
                                        var interimTranscript = '';
                                        var finalTranscript = '';
                                        
                                        for (var i = event.resultIndex; i < event.results.length; ++i) {
                                            var transcript = event.results[i][0].transcript;
                                            if (event.results[i].isFinal) {
                                                finalTranscript += transcript;
                                            } else {
                                                interimTranscript += transcript;
                                            }
                                        }
                                        
                                        var resultText = finalTranscript || interimTranscript;
                                        var isFinalResult = finalTranscript.length > 0;
                                        SpeechBridge.postResult(resultText, isFinalResult);
                                    };
                                    
                                    recognition.start();
                                } catch (e) {
                                    SpeechBridge.postError(e.message);
                                }
                            }
                            
                            function stopRecognition() {
                                if (recognition) {
                                    recognition.stop();
                                }
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                webView?.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize background WebView for Web Speech API", e)
                onError("Initialization failed: ${e.message}")
            }
        }
    }

    fun startListening(lang: String) {
        mainHandler.post {
            Log.d(TAG, "Request to start listening (lang=$lang)")
            webView?.evaluateJavascript("startRecognition('$lang');", null)
        }
    }

    fun stopListening() {
        mainHandler.post {
            Log.d(TAG, "Request to stop listening")
            webView?.evaluateJavascript("stopRecognition();", null)
        }
    }

    fun destroy() {
        mainHandler.post {
            Log.d(TAG, "Destroying background WebSpeechWebView")
            webView?.destroy()
            webView = null
        }
    }
}
