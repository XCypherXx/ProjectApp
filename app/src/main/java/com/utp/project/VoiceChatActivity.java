package com.utp.project;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class VoiceChatActivity extends AppCompatActivity
        implements VoiceToActivityProcessor.Callback {

    private static final int REQ_RECORD_AUDIO = 1001;
    private static final String EXTRA_AUTO_START_MIC = "auto_start_mic"; // Constante para el extra

    private VoiceToActivityProcessor voiceProcessor;
    private FirestoreActivityRepository repository;

    private FloatingActionButton btnStartVoice;
    private TextView tvMessage;
    private TextView tvListening;
    private View btnBack;
    private boolean shouldAutoStartMic = false; // Flag para activación automática
    private AudioWaveView audioWaveView;

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

        // Inicializar el visualizador
        audioWaveView = findViewById(R.id.audio_wave_view);

        voiceProcessor = new VoiceToActivityProcessor(this, this);
        repository = new FirestoreActivityRepository();

        btnStartVoice.setOnClickListener(v -> {
            if (checkAudioPermission()) {
                startVoiceFlow();
            } else {
                requestAudioPermission();
            }
        });
        // Verificamos si venimos de una agitación
        boolean autoStart = getIntent().getBooleanExtra("AUTO_START_MIC", false);

        if (autoStart) {
            // Si es automático, verificamos permiso y arrancamos de una vez
            if (checkAudioPermission()) {
                autoStartMicrophone(); // <--- Inicia la escucha sin pulsar el botón
            } else {
                requestAudioPermission();
            }
        }
    }

    private void programarNotificacion(ActivityData activityData) {
        try {
            // Obtener fecha y hora de inicio
            if (activityData.getDateStart() == null || activityData.getTimeStart() == null) {
                Log.w("VoiceChatActivity", "No hay fecha/hora de inicio, no se programa notificación");
                return;
            }

            // Parsear fecha y hora de inicio
            Calendar startCalendar = parseDateTimeToCalendar(
                    activityData.getDateStart(),
                    activityData.getTimeStart()
            );

            if (startCalendar == null) {
                Log.e("VoiceChatActivity", "Error al parsear fecha/hora de inicio");
                return;
            }

            // Obtener minutos antes de notificar (por defecto 15 minutos)
            int notificationMinutesBefore = activityData.getNotificarMinAntes() != null
                    ? activityData.getNotificarMinAntes()
                    : 15;

            // Obtener username desde SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String username = prefs.getString("username", "usuario");

            // Calcular hora de notificación
            Calendar notificationTime = (Calendar) startCalendar.clone();
            notificationTime.add(Calendar.MINUTE, -notificationMinutesBefore);

            // Forzar a que sea en punto (sin segundos ni milisegundos)
            notificationTime.set(Calendar.SECOND, 0);
            notificationTime.set(Calendar.MILLISECOND, 0);

            Log.d("VoiceChatActivity", "Hora de notificación calculada: " + notificationTime.getTime());
            Log.d("VoiceChatActivity", "Hora actual del sistema: " + new Date(System.currentTimeMillis()));

            if (notificationTime.getTimeInMillis() < System.currentTimeMillis()) {
                Log.w("VoiceChatActivity", "⚠ La hora de notificación ya pasó. No se programa.");
                return;
            }

            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            Log.d("VoiceChatActivity", "Notification ID generado: " + notificationId);

            // Formatear hora de inicio para el mensaje
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
            String horaInicio = sdf.format(startCalendar.getTime());

            // Crear mensaje para la notificación
            String mensaje = "Tu actividad \"" + activityData.getTitle() + "\" comienza a las " + horaInicio;
            if (activityData.getLocation() != null && !activityData.getLocation().isEmpty()) {
                mensaje += " en " + activityData.getLocation();
            }

            // Programar alarma
            NotificationReceiver.programarAlarma(
                    this,
                    notificationTime.getTimeInMillis(),
                    "Recordatorio: " + activityData.getTitle(),
                    mensaje,
                    notificationId,
                    username
            );

            Log.d("VoiceChatActivity", "Alarma programada correctamente");

        } catch (Exception e) {
            Log.e("VoiceChatActivity", "Error al programar notificación: " + e.getMessage(), e);
        }
    }

    private Calendar parseDateTimeToCalendar(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            Calendar calendar = Calendar.getInstance();

            // Parsear fecha
            String[] dateParts = dateStr.split("-");
            if (dateParts.length >= 3) {
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1; // Calendar.MONTH es 0-based
                int day = Integer.parseInt(dateParts[2]);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
            } else {
                return null;
            }

            // Parsear hora
            if (timeStr != null && !timeStr.isEmpty()) {
                String[] timeParts = timeStr.trim().split(":");
                if (timeParts.length >= 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                } else {
                    // Si no hay hora válida, usar medianoche
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                }
            } else {
                // Si no hay hora, usar medianoche
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
            }

            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar;
        } catch (Exception e) {
            Log.e("VoiceChatActivity", "Error al parsear fecha/hora: " + e.getMessage(), e);
            return null;
        }
    }

    private void autoStartMicrophone() {
        if (checkAudioPermission()) {
            // Ya tiene permiso, activar directamente
            startVoiceFlow();
        } else {
            // No tiene permiso, solicitarlo
            // Cuando se conceda, se activará automáticamente en onRequestPermissionsResult
            requestAudioPermission();
        }
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
                // NUEVO: Si venía del sensor de agitación, activar automáticamente
                if (shouldAutoStartMic) {
                    startVoiceFlow();
                } else {
                    // Comportamiento normal: solo activar si el usuario hizo click
                    // (aunque en este caso, si llegó aquí es porque hizo click, así que también activamos)
                    startVoiceFlow();
                }
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
                // NUEVO: Programar notificación con voz
                programarNotificacion(activityData);
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

    // --- Método obligatorio faltante ---
    @Override
    public void onListeningStateChanged(boolean isListening) {
        // Aquí puedes cambiar la UI si está escuchando o no
        runOnUiThread(() -> {
            if (isListening) {
                // Ejemplo: Cambiar ícono a "Stop" o mostrar animación
                tvListening.setVisibility(View.VISIBLE);
            } else {
                // Ejemplo: Ocultar animación
                tvListening.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAudioLevelUpdated(float rmsdB) {
        runOnUiThread(() -> {
            if (audioWaveView != null) {
                audioWaveView.updateAmplitude(rmsdB);
            }
        });
    }
}
