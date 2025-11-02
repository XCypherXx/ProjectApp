package com.utp.project.data;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirestoreService {

    private static final String COL_USUARIOS = "usuarios";
    private static final String SUB_ACTIVIDADES = "actividades";

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    @Nullable
    public static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public static DocumentReference userDoc() {
        String uid = uid();
        if (uid == null) throw new IllegalStateException("Usuario no autenticado.");
        return db().collection(COL_USUARIOS).document(uid);
    }

    public static Task<Void> ensureUserDocument(Map<String, Object> extra) {
        Map<String, Object> base = new HashMap<>();
        base.put("createdAt", FieldValue.serverTimestamp());
        base.put("ultimaSesion", FieldValue.serverTimestamp());
        if (extra != null) base.putAll(extra);
        return userDoc().set(base, SetOptions.merge());
    }

    public static Task<Void> updatePreferencias(Map<String, Object> prefs) {
        Map<String, Object> update = new HashMap<>();
        update.put("preferencias", prefs);
        return userDoc().set(update, SetOptions.merge());
    }

    public static CollectionReference actividadesCol() {
        return userDoc().collection(SUB_ACTIVIDADES);
    }

    public static Task<DocumentReference> addActividad(Map<String, Object> actividad) {
        if (!actividad.containsKey("createdAt")) {
            actividad.put("createdAt", FieldValue.serverTimestamp());
        }
        return actividadesCol().add(actividad);
    }

    public static Task<Void> updateActividad(String actividadId, Map<String, Object> campos) {
        return actividadesCol().document(actividadId).update(campos);
    }

    public static Task<Void> deleteActividad(String actividadId) {
        return actividadesCol().document(actividadId).delete();
    }

    public static ListenerRegistration listenActividades(EventListener<QuerySnapshot> listener) {
        return actividadesCol()
                .orderBy("fechaInicio", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public static Map<String, Object> buildActividad(
            String titulo,
            @Nullable Timestamp fechaInicio,
            @Nullable Timestamp fechaFin,
            @Nullable String descripcion,
            @Nullable String estado,
            @Nullable String categoria,
            @Nullable String direccion,
            @Nullable Double lat,
            @Nullable Double lon,
            int notificarMinAntes
    ) {
        Map<String, Object> a = new HashMap<>();
        a.put("titulo", titulo);
        if (fechaInicio != null) a.put("fechaInicio", fechaInicio);
        if (fechaFin != null) a.put("fechaFin", fechaFin);
        if (descripcion != null) a.put("descripcion", descripcion);
        a.put("estado", estado != null ? estado : "pendiente");
        if (categoria != null) a.put("categoria", categoria);
        if (direccion != null) a.put("direccion", direccion);
        if (lat != null) a.put("lat", lat);
        if (lon != null) a.put("lon", lon);
        a.put("notificarMinAntes", notificarMinAntes);
        return a;
    }
}



