package com.utp.project;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioWaveView extends View {

    private Paint paint;
    private List<Float> amplitudes = new ArrayList<>();
    private List<Float> spikeAmplitudes = new ArrayList<>(); // Para animación suave
    private float radius = 10f; // Ancho de las barras
    private float gap = 15f;    // Espacio entre barras
    private int maxSpikes = 0;  // Cuántas barras caben en pantalla

    // Configuración visual
    private int waveColor = Color.rgb(74, 144, 226); // Azul similar a tu tema

    public AudioWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(waveColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND); // Bordes redondeados
    }

    /**
     * Llama a este método con el valor RMS (dB) del SpeechRecognizer
     * Rango típico RMS: -2 a 10 (aprox)
     */
    public void updateAmplitude(float rmsdB) {
        // Normalizar el valor para que sea útil (0 a 100 aprox)
        float normalized = Math.max(0f, rmsdB * 10);

        // Agregar al historial
        if (amplitudes.size() >= maxSpikes) {
            amplitudes.remove(0);
        }
        amplitudes.add(normalized);

        // Invalidar para redibujar
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Calcular cuántas barras caben
        maxSpikes = (int) (w / (radius * 2 + gap));

        // Rellenar con ceros iniciales
        amplitudes.clear();
        for (int i = 0; i < maxSpikes; i++) {
            amplitudes.add(0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();
        int centerY = height / 2;

        float currentX = (width - (maxSpikes * (radius * 2 + gap))) / 2f; // Centrar horizontalmente

        for (float amp : amplitudes) {
            // Altura de la barra (escalada y con un mínimo para que siempre se vea un punto)
            float barHeight = Math.max(radius * 2, amp * 3);

            // Limitar altura máxima
            if (barHeight > height - 20) barHeight = height - 20;

            float top = centerY - (barHeight / 2);
            float bottom = centerY + (barHeight / 2);

            RectF rect = new RectF(currentX, top, currentX + (radius * 2), bottom);
            canvas.drawRoundRect(rect, radius, radius, paint);

            currentX += (radius * 2 + gap);
        }
    }
}