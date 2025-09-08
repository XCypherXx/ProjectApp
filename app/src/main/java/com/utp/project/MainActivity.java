package com.utp.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // logo

        new Handler().postDelayed(() -> {
            // Siempre abrir ActivityRegistro después del splash
            startActivity(new Intent(MainActivity.this, ActivityRegistro.class));
            finish(); // cerrar splash
        }, SPLASH_TIME);
    }
    }
