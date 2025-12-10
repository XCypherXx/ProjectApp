package com.utp.project.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Clase encargada de:
 * 1. Capturar voz con SpeechRecognizer.
 * 2. Convertir a texto.
 * 3. Enviar texto a Gemini 2.0 Flash.
 * 4. Devolver ActivityData a la Activity Java mediante callback.
 */
class VoiceToActivityProcessor @JvmOverloads constructor(
    private val context: Context,
    private val callback: Callback,
    private val geminiService: GeminiService = GeminiService.getInstance()
) : RecognitionListener {

    interface Callback {
        fun onVoiceProcessingStarted()
        fun onTextRecognized(rawText: String)
        fun onActivityParsed(activityData: ActivityData)
        fun onError(message: String)
        fun onListeningStateChanged(isListening: Boolean) // Ya lo tenías o agregalo si falta
        // NUEVO: Método para el volumen
        fun onAudioLevelUpdated(rmsdB: Float)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer?.setRecognitionListener(this)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE") // o "es-ES", ajusta a tu región
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora para crear una actividad...")
        }

        callback.onVoiceProcessingStarted()
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // ---- RecognitionListener ----

    override fun onReadyForSpeech(params: Bundle?) {
        // Puedes mostrar un indicador en UI desde la Activity usando el callback.
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Enviar el nivel de decibelios a la actividad
        callback.onAudioLevelUpdated(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        callback.onError("Error de reconocimiento de voz: código $error")
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()

        if (text.isNullOrBlank()) {
            callback.onError("No se pudo reconocer la voz. Intenta nuevamente.")
            return
        }

        callback.onTextRecognized(text)

        // Llamar a Gemini en background
        coroutineScope.launch {
            try {
                val activityData = geminiService.parseActivityFromText(text)
                callback.onActivityParsed(activityData)
            } catch (e: Exception) {
                callback.onError("Error al procesar el texto con Gemini: ${e.message}")
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}