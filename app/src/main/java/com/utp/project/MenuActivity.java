package com.utp.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MenuActivity extends AppCompatActivity {
    private MaterialButton btnRegistrarse;
    private MaterialButton btnIniciarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu); // archivo xml

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
