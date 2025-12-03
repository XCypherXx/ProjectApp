package com.utp.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.speech.tts.TextToSpeech;
public class NotificationReceiver extends BroadcastReceiver {
    private TextToSpeech tts;
    private static final String TAG = "NotificationReceiver";
    public static final String ACTION_SHOW_NOTIFICATION = "com.utp.project.ACTION_SHOW_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e(TAG, "========================================");
        Log.e(TAG, ">>> NOTIFICATION RECEIVER ACTIVADO <<<");
        Log.e(TAG, "========================================");

        if (intent == null) {
            Log.e(TAG, "ERROR: Intent es null!");
            return;
        }

        // Obtener datos
        String titulo = intent.getStringExtra("titulo");
        String mensaje = intent.getStringExtra("mensaje");
        String username = intent.getStringExtra("username"); // NUEVO
        int notificacionId = intent.getIntExtra("notificacion_id",
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE));

        if (titulo == null) titulo = "Recordatorio";
        if (mensaje == null) mensaje = "Tienes una actividad programada";
        if (username == null) username = "usuario";

        // ============================
        // 1. MOSTRAR NOTIFICACIÓN NORMAL
        // ============================
        NotificationHelper helper = new NotificationHelper(context);
        helper.showAlarmNotification(notificacionId, titulo, mensaje);

        // ============================
        // 2. HABLAR LA NOTIFICACIÓN (TTS)
        // ============================
        String mensajeHablado =
                "Hola " + username + ". " +
                        "Tienes una actividad pendiente, revisalo ahora. " +
                        mensaje;

        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new java.util.Locale("es", "ES"));

                // Preparar volumen máximo
                Bundle params = new Bundle();
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f); // 1.0 = máximo

                // Configurar listener para liberar TTS después de hablar
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        tts.shutdown(); // liberar TTS
                    }

                    @Override
                    public void onError(String utteranceId) {
                        tts.shutdown(); // liberar TTS si hay error
                    }
                });

                // Reproducir mensaje
                tts.speak(mensajeHablado, TextToSpeech.QUEUE_FLUSH, params, "ALARMA_TTS");

            } else {
                Log.e(TAG, "Error inicializando TTS");
            }
        });

    }

    // --- Método para programar alarma ---
    public static void programarAlarma(Context context, long triggerAtMillis,
                                       String titulo, String mensaje, int notificacionId,
                                       String username) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager es nulo, no se puede programar la alarma.");
                return;
            }

            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(ACTION_SHOW_NOTIFICATION);
            intent.putExtra("titulo", titulo);
            intent.putExtra("mensaje", mensaje);
            intent.putExtra("notificacion_id", notificacionId);
            intent.putExtra("username", username);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificacionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (triggerAtMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "La hora de la alarma ya pasó: " + triggerAtMillis);
            } else {
                Log.d(TAG, "Programando alarma futura: " + triggerAtMillis);
            }

            // --- Elegir método correcto según versión de Android ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.d(TAG, "Alarma programada con setExactAndAllowWhileIdle para ID: " + notificacionId);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.d(TAG, "Alarma programada con setExact para ID: " + notificacionId);
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.d(TAG, "Alarma programada con set (legacy) para ID: " + notificacionId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error programando alarma: " + e.getMessage(), e);
        }
    }

    // --- Método para cancelar alarma ---
    public static void cancelarAlarma(Context context, int notificacionId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager es nulo, no se puede cancelar la alarma.");
                return;
            }

            Intent intent = new Intent(context, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificacionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarma cancelada ID: " + notificacionId);

        } catch (Exception e) {
            Log.e(TAG, "Error al cancelar alarma: " + e.getMessage(), e);
        }
    }
}
