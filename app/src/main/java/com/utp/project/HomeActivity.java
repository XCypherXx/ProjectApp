package com.utp.project;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.utp.project.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {
    // private RecyclerView categoryRecyclerView;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_home);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
// Configuración de accesibilidad para FAB
        binding.fab.setFocusable(true);
        binding.fab.setClickable(true);
        binding.fab.setContentDescription(getString(R.string.fab_add_task_description));

// Listener del FABB
        binding.fab.setOnClickListener(v -> {
            Toast.makeText(this, "FAB presionado", Toast.LENGTH_SHORT).show();
            // Aquí puedes abrir un fragment o lanzar una Activity
        });


        // -------------------
        // BottomNavigation
        // -------------------

        // Pantalla inicial
        replaceFragment(new HomeFragment()); // fragment inicial

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (id == R.id.agenda) {
                replaceFragment(new CalendarFragment());
            } else if (id == R.id.microfono) {
                replaceFragment(new MicFragment());
            } else if (id == R.id.ajustes) {
                replaceFragment(new SettingFragment());
            }
            return true;
        });

        binding.fab.setOnClickListener(v -> {
            Toast.makeText(this, "FAB presionado", Toast.LENGTH_SHORT).show();


        });


    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }


}