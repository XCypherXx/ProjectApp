package com.utp.project.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utp.project.ai.ActivityData
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Guarda actividades en Firestore con la estructura:
 * users/{uid}/activities/{activityId}
 */
class FirestoreActivityRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    interface SaveCallback {
        fun onSuccess(activityId: String)
        fun onError(exception: Exception)
    }

    @JvmOverloads
    fun saveUserActivity(
        uid: String,
        activityData: ActivityData,
        callback: SaveCallback? = null
    ) {
        // Usar la misma estructura que FirestoreService: usuarios/{uid}/actividades
        val activitiesRef = db.collection("usuarios")
            .document(uid)
            .collection("actividades")

        // Convertir ActivityData al formato que espera la app
        val data = hashMapOf<String, Any>(
            "titulo" to activityData.title,
            "estado" to "pendiente" // estado por defecto
        )

        // Convertir fechas de String a Timestamp
        val fechaInicio = parseDateTimeToTimestamp(activityData.dateStart, activityData.timeStart)
        val fechaFin = parseDateTimeToTimestamp(activityData.dateEnd, activityData.timeEnd)

        if (fechaInicio != null) {
            data["fechaInicio"] = fechaInicio
        }
        if (fechaFin != null) {
            data["fechaFin"] = fechaFin
        }

        // Campos opcionales
        if (activityData.location != null) {
            data["direccion"] = activityData.location
        }
        if (activityData.categoria != null) {
            data["categoria"] = activityData.categoria
        }
        if (activityData.prioridad != null) {
            data["prioridad"] = activityData.prioridad.lowercase()
        }
        if (activityData.notes != null) {
            data["descripcion"] = activityData.notes
        }

        // Notificación: usar el valor detectado por Gemini, o 15 minutos por defecto si no hay
        data["notificarMinAntes"] = activityData.notificarMinAntes ?: 15

        // Agregar timestamp de creación
        data["createdAt"] = Timestamp.now()

        // Guardar usando add() para que Firestore genere el ID automáticamente
        activitiesRef.add(data)
            .addOnSuccessListener { documentReference ->
                callback?.onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                callback?.onError(e)
            }
    }

    /**
     * Convierte una fecha (String formato "YYYY-MM-DD") y hora (String formato "HH:mm")
     * a un Timestamp de Firestore.
     * Si no hay fecha, retorna null.
     */
    private fun parseDateTimeToTimestamp(dateStr: String?, timeStr: String?): Timestamp? {
        if (dateStr.isNullOrBlank()) {
            return null
        }

        try {
            // Intentar parsear con LocalDate primero (más robusto)
            val date = try {
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: DateTimeParseException) {
                // Si falla, intentar con SimpleDateFormat
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateFormat.parse(dateStr)?.let {
                    LocalDate.of(
                        it.year + 1900,
                        it.month + 1,
                        it.date
                    )
                } ?: return null
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, date.year)
            calendar.set(Calendar.MONTH, date.monthValue - 1)
            calendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)

            // Si hay hora, parsearla y aplicarla
            if (!timeStr.isNullOrBlank()) {
                val timeParts = timeStr.trim().split(":")
                if (timeParts.size >= 2) {
                    val hour = timeParts[0].toIntOrNull() ?: 0
                    val minute = timeParts[1].toIntOrNull() ?: 0
                    calendar.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                    calendar.set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
            } else {
                // Si no hay hora, usar medianoche
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            return Timestamp(calendar.time)
        } catch (e: Exception) {
            // Si hay error al parsear, retornar null
            android.util.Log.e("FirestoreActivityRepository", "Error al parsear fecha: ${e.message}")
            return null
        }
    }
}