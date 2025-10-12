package com.utp.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class MenuActivity extends AppCompatActivity {
    private MaterialButton btnRegistrarse;
    private MaterialButton btnIniciarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu); // archivo xml

        // usa el permiso, sin volver a solicitarlo
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            new NotificationHelper(this).showBasicNotification(1, "Registro", "Te estás registrando.");
        }


        // Obtener referencias a los botones del XML
        btnRegistrarse = findViewById(R.id.btn_registrarse);
        btnIniciarSesion = findViewById(R.id.button_iniciar_sesion);

        // Configurar el listener para el botón de "Registrarse"
        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Iniciar la actividad de registro
                Intent intent = new Intent(MenuActivity.this, ActivityRegistro.class);
                startActivity(intent);


                // Ejemplo: mostrar notificación local al registrarse
                NotificationHelper helper = new NotificationHelper(MenuActivity.this);
                helper.showBasicNotification(1, "Registro", "Estás por registrarte.");

            }
        });

         // Configurar el listener para el botón de "Iniciar Sesión"
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Iniciar la actividad de login (asumiendo que se llama ActivityLogin)
                Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
