package com.utp.project.data

import com.google.firebase.firestore.FirebaseFirestore
import com.utp.project.ai.ActivityData

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
        val activitiesRef = db.collection("users")
            .document(uid)
            .collection("activities")

        // Mapa que se guarda en Firestore
        val data = hashMapOf(
            "title" to activityData.title,
            "dateStart" to activityData.dateStart,
            "dateEnd" to activityData.dateEnd,
            "timeStart" to activityData.timeStart,
            "timeEnd" to activityData.timeEnd,
            "location" to activityData.location,
            "prioridad" to activityData.prioridad,
            "categoria" to activityData.categoria,
            "notes" to activityData.notes
        )

        val newDoc = activitiesRef.document()
        newDoc.set(data)
            .addOnSuccessListener {
                callback?.onSuccess(newDoc.id)
            }
            .addOnFailureListener { e ->
                callback?.onError(e)
            }
    }
}