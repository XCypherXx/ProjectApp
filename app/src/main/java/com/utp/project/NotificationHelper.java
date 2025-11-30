package com.utp.project;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "ACTIVIDADES_ALARMA_V3";

    private final Context ctx;

    public NotificationHelper(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        createChannelIfNeeded();
    }

    // Crear canal de notificación si es Android O+
    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Log.d("NotificationHelper", "Entrando a createChannelIfNeeded()");

            NotificationManager manager =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager == null) {
                Log.e("NotificationHelper", "ERROR: NotificationManager es NULL");
                return;
            }

            NotificationChannel canalExistente = manager.getNotificationChannel(CHANNEL_ID);

            if (canalExistente != null) {
                Log.w("NotificationHelper", "El canal YA EXISTE: " + CHANNEL_ID);
                Log.w("NotificationHelper", "Importancia actual: " + canalExistente.getImportance());
                Log.w("NotificationHelper", "Sonido actual: " + canalExistente.getSound());
                return;
            }

            Log.e("NotificationHelper", "CREANDO CANAL NUEVO: " + CHANNEL_ID);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de Actividades",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Notificaciones para actividades programadas");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            channel.setSound(sonido, audioAttributes);

            manager.createNotificationChannel(channel);

            Log.e("NotificationHelper", " CANAL CREADO CON SONIDO Y VIBRACIÓN ");
        }
    }

    // -------------------------------------------------------------------------
    // NOTIFICACIÓN BÁSICA (para MainActivity o receiver simple)
    // -------------------------------------------------------------------------
    public void showBasicNotification(int id, String title, String text) {
        Log.d(TAG, "Mostrando notificación básica ID: " + id);

        if (!tienePermiso()) {
            Log.e(TAG, "No hay permiso para mostrar notificaciones básicas");
            return;
        }

        Intent intent = new Intent(ctx, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_ID)
                        .setSmallIcon(R.drawable.icono_agenda)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
            Log.d(TAG, "Notificación básica mostrada ID: " + id);
        } catch (SecurityException e) {
            Log.e(TAG, "Error mostrando notificación básica: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // NOTIFICACIÓN DE ALARMA (sonido + vibración + popup)
    // -------------------------------------------------------------------------
    public void showAlarmNotification(int id, String title, String text) {
        Log.d(TAG, "Mostrando notificación de alarma ID: " + id);

        if (!tienePermiso()) {
            Log.e(TAG, "No hay permiso para mostrar notificaciones de alarma");
            return;
        }

        Intent intent = new Intent(ctx, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sonido == null) sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.icono_agenda)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // NUEVO: indica que es alarma
                .setVibrate(new long[]{0, 500, 200, 500})      // NUEVO: vibración
                .setSound(sonido)                               // NUEVO: sonido
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
               // NUEVO: popup sobre pantalla bloqueada

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
            Log.d(TAG, "Notificación de alarma mostrada con sonido y popup ID: " + id);
        } catch (SecurityException e) {
            Log.e(TAG, "Error mostrando notificación de alarma: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PROGRAMAR ALARMA CON ALARM MANAGER
    // -------------------------------------------------------------------------
    public void programarAlarma(long triggerAtMillis, String titulo, String mensaje, int id) {
        Log.d(TAG, "Programando alarma ID: " + id + " para: " + triggerAtMillis);

        Intent intent = new Intent(ctx, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_SHOW_NOTIFICATION);
        intent.putExtra("titulo", titulo);
        intent.putExtra("mensaje", mensaje);
        intent.putExtra("notificacion_id", id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                ctx,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
            Log.d(TAG, "Alarma programada correctamente ID: " + id);
        } else {
            Log.e(TAG, "AlarmManager es null, no se pudo programar alarma");
        }
    }

    // -------------------------------------------------------------------------
    // CANCELAR ALARMA
    // -------------------------------------------------------------------------
    public void cancelarAlarma(int id) {
        Log.d(TAG, "Cancelando alarma ID: " + id);

        Intent intent = new Intent(ctx, NotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                ctx,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarma cancelada correctamente ID: " + id);
        } else {
            Log.e(TAG, "AlarmManager es null, no se pudo cancelar alarma");
        }
    }

    // -------------------------------------------------------------------------
    // VERIFICAR PERMISO DE NOTIFICACIONES
    // -------------------------------------------------------------------------
    private boolean tienePermiso() {

        // Android 12 o menor → permiso automático
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android < 13 → Permiso de notificaciones automático");
            return true;
        }

        // Android 13 o mayor → verificar permiso real
        boolean permitido = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;

        if (!permitido) {
            Log.e(TAG, "NO hay permiso para notificaciones (Android 13+)");
        } else {
            Log.d(TAG, "Permiso de notificaciones CONCEDIDO");
    }

        return permitido;
    }
}
