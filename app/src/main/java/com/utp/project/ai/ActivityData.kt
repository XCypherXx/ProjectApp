package com.utp.project.ai

data class ActivityData(
    val title: String,
    val dateStart: String?,   // puede ser null si no se detecta
    val dateEnd: String?,
    val timeStart: String?,
    val timeEnd: String?,
    val location: String?,
    val prioridad: String?,   // prioridad (ej: "alta", "media", "baja")
    val categoria: String?,   // ej: "cita médica", "reunión", etc.
    val notes: String?        // campo libre
)