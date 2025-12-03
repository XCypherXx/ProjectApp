package com.utp.project.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.utp.project.BuildConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return """
Eres un asistente que interpreta texto hablado y lo convierte en una actividad de agenda.

FECHA ACTUAL: $todayStr (usa esta como referencia para fechas relativas como "mañana", "lunes", etc.)

Reglas IMPORTANTES:
- Devuelve SIEMPRE SOLO un JSON VÁLIDO (sin texto adicional, sin explicación).
- Las fechas DEBEN estar en formato "YYYY-MM-DD" (ejemplo: "2025-01-15").
- Las horas DEBEN estar en formato "HH:mm" en 24 horas (ejemplo: "15:30" para las 3:30 PM).
- Si el usuario dice "mañana", calcula la fecha de mañana basándote en la fecha actual.
- Si el usuario dice "lunes", "martes", etc., calcula la próxima ocurrencia de ese día.
- Si no hay fecha clara, usa null en "dateStart" y "dateEnd".
- Si no hay hora clara, usa null en "timeStart" y "timeEnd".
- Si no hay lugar, usa null en "location".
- "prioridad" puede ser "alta", "media", "baja" o null si no se puede inferir.
- "categoria" es una etiqueta corta, por ejemplo: "cita médica", "reunión", "estudio", etc., o null si no se puede inferir.
- "notes" puede contener información adicional que no encaje en los otros campos, o null.
- "notificarMinAntes": Si el usuario menciona cuándo quiere ser notificado (ej: "avísame 30 minutos antes", "notifícame 1 hora antes"), convierte eso a minutos. Si no menciona nada, usa null.

Ejemplos de conversión de fechas relativas:
- "mañana" → fecha de mañana
- "pasado mañana" → fecha de pasado mañana
- "lunes" → próximo lunes desde hoy
- "el 15 de enero" → "2025-01-15" (ajusta el año si es necesario)

Ejemplos de conversión de horas:
- "3pm" o "3 PM" → "15:00"
- "9:30am" → "09:30"
- "las 2 de la tarde" → "14:00"

Devuelve SIEMPRE una respuesta JSON con estos campos:
title, dateStart, dateEnd, timeStart, timeEnd, location, prioridad, categoria, notes, notificarMinAntes.

Ejemplo de formato esperado:
{
  "title": "Cita médica con el Dr. Pérez",
  "dateStart": "2025-01-15",
  "dateEnd": "2025-01-15",
  "timeStart": "15:00",
  "timeEnd": "16:00",
  "location": "Clínica San Juan",
  "prioridad": "alta",
  "categoria": "cita médica",
  "notes": "Llevar resultados de laboratorio",
  "notificarMinAntes": 30
}

Texto del usuario: "$userInput"
        """.trimIndent()
    }

    /**
     * Intenta encontrar el JSON en la respuesta y convertirlo a ActivityData.
     * Ignora texto fuera de llaves si el modelo llegara a agregar algo.
     */
    private fun parseJsonToActivityData(rawResponse: String): ActivityData {
        val jsonString = extractFirstJsonObject(rawResponse)
        val json = JSONObject(jsonString)

        fun JSONObject.optNullableString(key: String): String? {
            return if (has(key) && !isNull(key)) optString(key, null) else null
        }

        fun JSONObject.optNullableInt(key: String): Int? {
            return if (has(key) && !isNull(key)) {
                try {
                    optInt(key, 0).takeIf { it > 0 }
                } catch (e: Exception) {
                    null
                }
            } else null
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
        val notificarMinAntes = json.optNullableInt("notificarMinAntes")

        return ActivityData(
            title = title,
            dateStart = dateStart,
            dateEnd = dateEnd,
            timeStart = timeStart,
            timeEnd = timeEnd,
            location = location,
            prioridad = prioridad,
            categoria = categoria,
            notes = notes,
            notificarMinAntes = notificarMinAntes
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