package com.utp.project;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.utp.project.data.FirestoreService;
import com.utp.project.databinding.ActivityHomeBinding;

//Importacion para Place SDK
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {
    // private RecyclerView categoryRecyclerView;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;
    private ActivityHomeBinding binding;

    private static final String GOOGLE_API_KEY="AIzaSyA0LzOGh05nahYKpb3q0gsYxhdryuyMhBc";

    // --- Lógica del Sensor de Agitación ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    // El umbral de 800 es un valor de ejemplo. Puedes ajustarlo para más o menos sensibilidad.
    private static final int SHAKE_THRESHOLD = 2000;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    // --- Fin Lógica del Sensor de Agitación ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_home);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // NUEVO: garantizar sesión (anónima si no hay) y merge de documento usuario
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null) {
            FirestoreService.ensureUserDocument(null);
        } else {
            // Si no hay usuario, devuélvelo al login.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        // Solo inicializa si no ha sido inicializado antes
        if (!Places.isInitialized() && !GOOGLE_API_KEY.isEmpty()) {
            // Usa el contexto de la aplicación y la clave API
            Places.initialize(getApplicationContext(), GOOGLE_API_KEY);
        } else if (GOOGLE_API_KEY.isEmpty()){
            Toast.makeText(this, "ERROR: La clave API de Google no está configurada en HomeActivity.", Toast.LENGTH_LONG).show();
        }

        // Configuración de accesibilidad para FAB
        binding.fab.setFocusable(true);
        binding.fab.setClickable(true);
        binding.fab.setContentDescription(getString(R.string.fab_add_task_description));




        // -------------------
        // BottomNavigation
        // -------------------

        // Pantalla inicial
        replaceFragment(new HomeFragment()); // fragment inicial

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            //java homefagment
            if (id == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (id == R.id.agenda) {
                replaceFragment(new CalendarFragment());
            } else if (id == R.id.agregar) {
                replaceFragment(new AddFragment());
            } else if (id == R.id.ajustes) {
                replaceFragment(new SettingFragment());
            }
            return true;
        });

        binding.fab.setOnClickListener(v -> {
            // 1) log / toast para confirmar ejecución
            Log.d("FAB_DEBUG", "FAB click recibido en HomeActivity");
            Toast.makeText(this, "escuchando", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, VoiceChatActivity.class);
            startActivity(intent);

        });


        // Inicialización del Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    // Lógica del Sensor de Agitación (SensorEventListener)

    @Override
    public void onResume() {
        super.onResume();
        // Iniciar la escucha del sensor cuando la actividad esté activa
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Detener la escucha del sensor para ahorrar batería
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();

            // Verificación cada 100ms para evitar spam de eventos
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Cálculo de la magnitud de la agitación
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    // ¡Agitación detectada!
                    handleShakeDetection();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se requiere implementación para este caso.
    }

    // Manejo de Agitación y Activación del Micrófono

    private void handleShakeDetection() {
        // 1. Deshabilitar el sensor temporalmente para evitar que se dispare varias veces
        sensorManager.unregisterListener(this);

        // 2. Dar feedback de vibración al usuario
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(100);
        }

        // 3. Verificar y solicitar permiso de micrófono
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Solicitar permiso
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            // Permiso concedido, activar la interfaz de micrófono
            activateMicrophone();
        }
    }

    private void activateMicrophone() {
        Toast.makeText(this, "Micrófono Activado por Agitación", Toast.LENGTH_SHORT).show();

        // Abrir VoiceChatActivity con un flag que indique activación automática
        Intent intent = new Intent(HomeActivity.this, VoiceChatActivity.class);
        intent.putExtra("auto_start_mic", true); // Flag para activación automática
        // AGREGAMOS LA SEÑAL: "true" significa que debe arrancar solo
        intent.putExtra("AUTO_START_MIC", true);
        startActivity(intent);
    }

    // Manejo del Resultado de la Solicitud de Permisos

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, activar la funcionalidad
                activateMicrophone();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de micrófono denegado.", Toast.LENGTH_SHORT).show();
            }
        }

        // Volver a registrar el sensor (lo hacemos aquí para asegurar que se reactive después del permiso)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }
}