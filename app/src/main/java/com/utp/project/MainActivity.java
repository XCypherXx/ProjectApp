package com.utp.project;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 5000;
    private static final int REQ_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MAIN", " MainActivity iniciada");

        // ============================
        //  PEDIR PERMISO DE NOTIFICACIÓN BIEN HECHO
        // ============================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                Log.w("PERMISOS", " Permiso NO concedido, solicitando...");
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATIONS
                );

            } else {
                Log.d("PERMISOS", " Permiso YA estaba concedido");
                lanzarNotificacionPrueba(); // SOLO si ya hay permiso
            }

        } else {
            // Android 12 o menor no necesita permiso
            lanzarNotificacionPrueba();
        }

        // ============================
        // UI SPLASH
        // ============================
        TextView nameCreater1 = findViewById(R.id.nameCreater1);
        TextView nameCreater2 = findViewById(R.id.nameCreater2);

        nameCreater1.post(() -> {
            int width = nameCreater1.getMeasuredWidth();
            int[] gradientColors = {
                    Color.parseColor("#FF3632D5"),
                    Color.parseColor("#FFEFA2FF")
            };
            Shader textShader = new LinearGradient(
                    0, 0,
                    width, 0,
                    gradientColors,
                    null,
                    Shader.TileMode.CLAMP
            );
            nameCreater1.getPaint().setShader(textShader);
            nameCreater1.invalidate();
        });

        nameCreater2.post(() -> {
            int width = nameCreater2.getMeasuredWidth();
            int[] gradientColors = {
                    Color.parseColor("#FF3632D5"),
                    Color.parseColor("#FFEFA2FF")
            };
            Shader textShader = new LinearGradient(
                    0, 0,
                    width, 0,
                    gradientColors,
                    null,
                    Shader.TileMode.CLAMP
            );
            nameCreater2.getPaint().setShader(textShader);
            nameCreater2.invalidate();
        });

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

            if (isFirstTime) {
                startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, MenuActivity.class));
            }
            finish();
        }, SPLASH_TIME);
    }

    // ============================
    //  RESULTADO DEL PERMISO
    // ============================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_NOTIFICATIONS) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.d("PERMISOS", " Usuario CONCEDIÓ notificaciones");
                lanzarNotificacionPrueba();

            } else {
                Log.e("PERMISOS", " Usuario DENEGÓ notificaciones");
            }
        }
    }

    // ============================
    // NOTIFICACIÓN DE PRUEBA CORRECTA
    // ============================
    private void lanzarNotificacionPrueba() {
        Log.d("MAIN", " Lanzando notificación de prueba");

        new NotificationHelper(this).showAlarmNotification(
                999,
                "PRUEBA URGENTE",
                "Si vess esto, las notificaciones SI funcionan"
        );
    }
}
