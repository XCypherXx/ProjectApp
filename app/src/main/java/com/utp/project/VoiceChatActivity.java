package com.utp.project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.utp.project.R;
import com.utp.project.ai.ActivityData;
import com.utp.project.ai.VoiceToActivityProcessor;
import com.utp.project.data.FirestoreActivityRepository;

public class VoiceChatActivity extends AppCompatActivity
        implements VoiceToActivityProcessor.Callback {

    private static final int REQ_RECORD_AUDIO = 1001;

    private VoiceToActivityProcessor voiceProcessor;
    private FirestoreActivityRepository repository;

    private FloatingActionButton btnStartVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // crea este layout con un botón

        btnStartVoice = findViewById(R.id.fab);

        voiceProcessor = new VoiceToActivityProcessor(this, this);
        repository = new FirestoreActivityRepository();

        btnStartVoice.setOnClickListener(v -> {
            if (checkAudioPermission()) {
                startVoiceFlow();
            } else {
                requestAudioPermission();
            }
        });
    }

    private void startVoiceFlow() {
        voiceProcessor.startListening();
    }

    private boolean checkAudioPermission() {
        int permission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
        );
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_RECORD_AUDIO
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceProcessor != null) {
            voiceProcessor.release();
        }
    }

    // ---- Manejo de permisos ----

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceFlow();
            } else {
                Toast.makeText(this,
                        "Se requiere permiso de micrófono para usar esta función.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---- Callbacks de VoiceToActivityProcessor ----

    @Override
    public void onVoiceProcessingStarted() {
        Toast.makeText(this, "Escuchando... habla ahora.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTextRecognized(String rawText) {
        Toast.makeText(this, "Texto reconocido: " + rawText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityParsed(ActivityData activityData) {
        Toast.makeText(this,
                "Actividad generada: " + activityData.getTitle(),
                Toast.LENGTH_LONG).show();

        // Aquí guardamos en Firestore
        String uid = getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this,
                    "Usuario no autenticado. No se puede guardar en Firestore.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        repository.saveUserActivity(uid, activityData, new FirestoreActivityRepository.SaveCallback() {
            @Override
            public void onSuccess(String activityId) {
                Toast.makeText(VoiceChatActivity.this,
                        "Actividad guardada con id: " + activityId,
                        Toast.LENGTH_LONG).show();
                // Aquí tu calendario puede refrescar datos si lo deseas.
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(VoiceChatActivity.this,
                        "Error al guardar en Firestore: " + exception.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }
}
