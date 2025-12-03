package com.utp.project.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.utp.project.BuildConfig

class GeminiService private constructor(
    private val generativeModel: GenerativeModel
) {

    /**
     * Convierte texto libre (voz transcrita) a ActivityData usando Gemini 2.0 Flash.
     * Lanza excepción si ocurre algún error de red o parsing.
     */
    suspend fun parseActivityFromText(userInput: String): ActivityData =
        withContext(Dispatchers.IO) {
            if (userInput.trim().length < 8) {
                throw IllegalArgumentException("El texto es demasiado corto para crear una actividad.")
            }

            val prompt = buildPrompt(userInput)

            // CORRECCIÓN: Pasar el prompt directamente (la librería se encarga del resto)
            val response = generativeModel.generateContent(prompt)

            val text = response.text ?: throw IllegalStateException("Gemini no devolvió texto.")
            parseJsonToActivityData(text)
        }

    private fun buildPrompt(userInput: String): String {
        return """
Eres un asistente que interpreta texto hablado y lo convierte en una actividad de agenda.

Reglas IMPORTANTES:
- Devuelve SIEMPRE SOLO un JSON VÁLIDO (sin texto adicional, sin explicación).
- Si no hay fecha clara, usa null en "dateStart" y "dateEnd".
- Si no hay hora clara, usa null en "timeStart" y "timeEnd".
- Si no hay lugar, usa null en "location".
- "prioridad" puede ser "alta", "media", "baja" o null si no se puede inferir.
- "categoria" es una etiqueta corta, por ejemplo: "cita médica", "reunión", "estudio", etc., o null si no se puede inferir.
- "notes" puede contener información adicional que no encaje en los otros campos, o null.

Devuelve SIEMPRE una respuesta JSON con estos campos:
title, dateStart, dateEnd, timeStart, timeEnd, location, prioridad, categoria, notes.

Ejemplo de formato esperado (solo formato, no lo uses literalmente):

{
  "title": "Cita médica con el Dr. Pérez",
  "dateStart": "2025-05-12",
  "dateEnd": "2025-05-12",
  "timeStart": "15:00",
  "timeEnd": "16:00",
  "location": "Clínica San Juan",
  "prioridad": "alta",
  "categoria": "cita médica",
  "notes": "Llevar resultados de laboratorio"
}

Texto del usuario: "$userInput"
        """.trimIndent()
    }

    /**
     * Intenta encontrar el JSON en la respuesta y convertirlo a ActivityData.
     * Ignora texto fuera de llaves si el modelo llegara a agregar algo.
     */
    private fun parseJsonToActivityData(rawResponse: String): ActivityData {
        // Tratar de aislar el bloque JSON principal
        val jsonString = extractFirstJsonObject(rawResponse)

        val json = JSONObject(jsonString)

        fun JSONObject.optNullableString(key: String): String? {
            return if (has(key) && !isNull(key)) optString(key, null) else null
        }

        val title = json.optString("title", "").ifBlank {
            throw IllegalStateException("Gemini no devolvió un título para la actividad.")
        }

        val dateStart = json.optNullableString("dateStart")
        val dateEnd = json.optNullableString("dateEnd")
        val timeStart = json.optNullableString("timeStart")
        val timeEnd = json.optNullableString("timeEnd")
        val location = json.optNullableString("location")
        val prioridad = json.optNullableString("prioridad")
        val categoria = json.optNullableString("categoria")
        val notes = json.optNullableString("notes")

        return ActivityData(
            title = title,
            dateStart = dateStart,
            dateEnd = dateEnd,
            timeStart = timeStart,
            timeEnd = timeEnd,
            location = location,
            prioridad = prioridad,
            categoria = categoria,
            notes = notes
        )
    }

    private fun extractFirstJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            throw IllegalStateException("No se encontró un JSON válido en la respuesta de Gemini.")
        }
        return text.substring(start, end + 1)
    }

    companion object {
        @Volatile
        private var INSTANCE: GeminiService? = null

        @JvmStatic
        fun getInstance(): GeminiService {
            return INSTANCE ?: synchronized(this) {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    throw IllegalStateException("GEMINI_API_KEY no configurado en BuildConfig.")
                }
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = apiKey
                )
                GeminiService(model).also { INSTANCE = it }
            }
        }
    }
}