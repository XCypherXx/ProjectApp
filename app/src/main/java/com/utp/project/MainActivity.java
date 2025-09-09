package com.utp.project;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // logo

        // Referencia a la primera TextView (nameCreater1)
        TextView nameCreater1 = findViewById(R.id.nameCreater1);

        // Referencia a la segunda TextView (appName)
        TextView nameCreater2 = findViewById(R.id.nameCreater2);

        // --- Aplica el degradado a la primera TextView (nameCreater1) ---
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

        // --- Aplica el degradado a la segunda TextView (appName) ---
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
            // Siempre abrir ActivityRegistro después del splash
            startActivity(new Intent(MainActivity.this, ActivityRegistro.class));
            finish(); // cerrar splash
        }, SPLASH_TIME);


    }


}
