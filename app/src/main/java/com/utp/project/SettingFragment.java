package com.utp.project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
public class SettingFragment extends Fragment {

   // private Switch switchNotificaciones;
private MaterialSwitch switchNotificaciones;
    private TextView userNameText, userEmailText;
    private ShapeableImageView profileImage;

    public SettingFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Inicializar vistas
        userNameText = view.findViewById(R.id.user_name);
        userEmailText = view.findViewById(R.id.user_email);
        profileImage = view.findViewById(R.id.profile_image);
        switchNotificaciones = view.findViewById(R.id.switch_notifications);

        // 🔹 Cargar datos del usuario actual
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userNameText.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            userEmailText.setText(user.getEmail());

            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                Picasso.get().load(photoUrl).into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_user_default);
            }
        }else {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
            String name = prefs.getString("nombre", "Usuario");
            String email = prefs.getString("correo", "usuario@mail.com");

            userNameText.setText(name);
            userEmailText.setText(email);
            profileImage.setImageResource(R.drawable.ic_user_default);
        }

        // Estado del switch
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("settings", requireContext().MODE_PRIVATE);

        boolean notificacionesActivas = prefs.getBoolean("notifications_enabled", true);
        switchNotificaciones.setChecked(notificacionesActivas);

        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();

            // Mostrar mensaje dentro de la app (sin lanzar otra Activity)
            if (isChecked) {
                Toast.makeText(requireContext(),
                        "🔔 Notificaciones activadas",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "🔇 Notificaciones silenciadas temporalmente",
                        Toast.LENGTH_SHORT).show();
            }
        });


        View itemLogout = view.findViewById(R.id.item_logout);

        itemLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Deseas cerrar tu sesión actual?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Cerrar sesión en Firebase
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

                        // clear prefs locales
                        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
                        userPrefs.edit().clear().apply();

                        // Volver al menú principal
                        Intent intent = new Intent(requireContext(), MenuActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        });



    }
}
