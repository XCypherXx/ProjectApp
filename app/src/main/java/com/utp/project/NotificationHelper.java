package com.utp.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    public static final String CHANNEL_ID = "CANAL_UTP";
    private final Context ctx;

    public NotificationHelper(Context ctx) {
        this.ctx = ctx instanceof android.app.Application ? ctx : ctx.getApplicationContext();

        createChannelIfNeeded();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = ctx.getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Canal de prueba",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Canal para notificaciones de la app");
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showBasicNotification(int id, String title, String text) {

        //  1. Verificar si el usuario las tiene activadas
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean notificacionesActivas = prefs.getBoolean("notificaciones_activas", true);

        if (!notificacionesActivas) {
            // Si están desactivadas, no mostrar nada
            return;
        }


        // 2. Crear la notificación normalmente
        PendingIntent emptyIntent = PendingIntent.getActivity(
                ctx, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(emptyIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Solo mostrar si el permiso está concedido
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
        }

    }
}

