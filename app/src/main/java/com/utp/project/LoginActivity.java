package com.utp.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsuario;
    private EditText editTextPassword;
    private MaterialButton buttonIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Obtener referencias de los elementos del layout
        editTextUsuario = findViewById(R.id.editText_usuario);
        editTextPassword = findViewById(R.id.editText_password);
        buttonIngresar = findViewById(R.id.button_ingresar);

        buttonIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener datos ingresados por el usuario
                String usuarioIngresado = editTextUsuario.getText().toString().trim();
                String passwordIngresada = editTextPassword.getText().toString().trim();

                // Obtener los datos guardados en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String usuarioGuardado = prefs.getString("username", "");
                String passwordGuardada = prefs.getString("password", "");

                // Validar credenciales
                if (usuarioIngresado.equals(usuarioGuardado) && passwordIngresada.equals(passwordGuardada)) {
                    // Credenciales correctas, ir a la pantalla principal
                    Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish(); // Cierra la actividad de login
                } else {
                    // Credenciales incorrectas, mostrar un mensaje de error
                    Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}