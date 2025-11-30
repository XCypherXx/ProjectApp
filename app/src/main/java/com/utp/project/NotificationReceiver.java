package com.utp.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

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

        String action = intent.getAction();
        Log.d(TAG, "Action recibida: " + action);

        // --- Adquirir WakeLock para asegurar que la notificación se muestre ---
        PowerManager.WakeLock wakeLock = null;
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "NotificationReceiver::WakeLock"
                );
                wakeLock.acquire(5 * 60 * 1000L); // 5 minutos
                Log.d(TAG, "WakeLock adquirido");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al adquirir WakeLock: " + e.getMessage(), e);
        }

        try {
            // --- Obtener datos de la notificación ---
            String titulo = intent.getStringExtra("titulo");
            String mensaje = intent.getStringExtra("mensaje");
            int notificacionId = intent.getIntExtra("notificacion_id",
                    (int) (System.currentTimeMillis() % Integer.MAX_VALUE));

            if (titulo == null || titulo.isEmpty()) titulo = "Recordatorio";
            if (mensaje == null || mensaje.isEmpty()) mensaje = "Tienes una actividad programada";

            Log.d(TAG, "Mostrando notificación con ID: " + notificacionId + " | Título: " + titulo + " | Mensaje: " + mensaje);

            // --- Mostrar notificación COMPLETA (popup + sonido + vibración) ---
            NotificationHelper helper = new NotificationHelper(context);
            helper.showAlarmNotification(notificacionId, titulo, mensaje);

            Log.e(TAG, "✓ NOTIFICACIÓN MOSTRADA EXITOSAMENTE");

        } catch (Exception e) {
            Log.e(TAG, "ERROR al mostrar notificación: " + e.getMessage(), e);
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock liberado");
            }
        }
    }

    // --- Método para programar alarma ---
    public static void programarAlarma(Context context, long triggerAtMillis,
                                       String titulo, String mensaje, int notificacionId) {
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
