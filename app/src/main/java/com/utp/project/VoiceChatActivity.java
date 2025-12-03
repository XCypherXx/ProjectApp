package com.utp.project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
    private TextView tvMessage;
    private TextView tvListening;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat); // CAMBIO: usar activity_voice_chat

        btnStartVoice = findViewById(R.id.btn_mic); // CAMBIO: usar btn_mic del layout
        tvMessage = findViewById(R.id.tv_message);
        tvListening = findViewById(R.id.tv_listening);
        btnBack = findViewById(R.id.btn_back);

        // Botón de volver
        btnBack.setOnClickListener(v -> finish());

        // Inicialmente ocultar el texto "Escuchando..."
        tvListening.setVisibility(View.GONE);

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
        runOnUiThread(() -> {
            tvMessage.setText("🎤 Escuchando... habla ahora.");
            tvListening.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onTextRecognized(String rawText) {
        runOnUiThread(() -> {
            tvMessage.setText("✅ Texto reconocido:\n" + rawText);
            tvListening.setVisibility(View.GONE);
        });
    }

    @Override
    public void onActivityParsed(ActivityData activityData) {
        runOnUiThread(() -> {
            tvMessage.setText("✨ Procesando actividad...");
        });

        // Aquí guardamos en Firestore
        String uid = getCurrentUserId();
        if (uid == null) {
            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Usuario no autenticado. No se puede guardar en Firestore.",
                        Toast.LENGTH_LONG).show();
                tvMessage.setText("❌ Error: Usuario no autenticado");
            });
            return;
        }

        repository.saveUserActivity(uid, activityData, new FirestoreActivityRepository.SaveCallback() {
            @Override
            public void onSuccess(String activityId) {
                runOnUiThread(() -> {
                    Toast.makeText(VoiceChatActivity.this,
                            "✅ Actividad guardada exitosamente",
                            Toast.LENGTH_LONG).show();
                    tvMessage.setText("✅ Actividad creada:\n" + activityData.getTitle());
                    // Cerrar después de 2 segundos
                    btnStartVoice.postDelayed(() -> finish(), 2000);
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(VoiceChatActivity.this,
                            "❌ Error al guardar: " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                    tvMessage.setText("❌ Error al guardar la actividad");
                });
            }
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            tvMessage.setText("❌ " + message);
            tvListening.setVisibility(View.GONE);
        });
    }

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }
}
