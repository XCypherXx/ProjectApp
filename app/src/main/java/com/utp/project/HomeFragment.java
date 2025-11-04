package com.utp.project;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.project.databinding.ActivityHomeBinding;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.utp.project.R;

import android.app.ActivityOptions;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.utp.project.databinding.ActivityHomeBinding;
import com.utp.project.databinding.FragmentHomeBinding;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.utp.project.adapters.PlanAdapter;
import com.utp.project.models.PlanModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import androidx.recyclerview.widget.RecyclerView;


public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    // private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ArrayList<Category> categoryList;

    // Variables para actividades
    private PlanAdapter actividadesAdapter;
    private List<PlanModel> actividadesList;
    private ListenerRegistration actividadesListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // setContentView(R.layout.activity_home);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // TextView userGreeting = findViewById(R.id.user_greeting);
        // ImageView profileImage = findViewById(R.id.profile_image);

        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String currentUser = prefs.getString("currentUser", null);
                name = (currentUser != null && !currentUser.isEmpty()) ? currentUser : "admin";
            }
            binding.userGreeting.setText("Hola, " + name);

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .into(binding.profileImage);
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_user_default);
            }
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUser = prefs.getString("currentUser", "admin");
            binding.userGreeting.setText("Hola, " + currentUser + " 👋");
            binding.profileImage.setImageResource(R.drawable.ic_user_default);
        }


        // categoryRecyclerView = findViewById(R.id.categoryRecyclerView);

        // Lista dinámica de categorías (se llenará desde Firestore)
        categoryList = new ArrayList<>();

        categoryAdapter = new CategoryAdapter(getContext(), categoryList);
        binding.categoryRecyclerView.setAdapter(categoryAdapter);

        // Layout horizontal slider de cards
        binding.categoryRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        // Clicks en las cards
        categoryAdapter.setOnItemClickListener(position -> {
            Category clicked = categoryList.get(position);
            Toast.makeText(getContext(), "Clicked: " + clicked.getName(), Toast.LENGTH_SHORT).show();
        });

        // Configurar contenedor de actividades
        // (reemplaza al RecyclerView por tarjetas MaterialCardView dinámicas)

        actividadesList = new ArrayList<>();
        // Cargar actividades desde Firestore
        loadActividadesFromFirestore();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar el listener cuando se destruye la vista
        if (actividadesListener != null) {
            actividadesListener.remove();
        }
    }

    private void renderActividadesEnTarjetas(View root) {
        android.widget.LinearLayout container = root.findViewById(R.id.container_actividades_home);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (PlanModel plan : actividadesList) {
            android.view.View card = inflater.inflate(R.layout.item_home_material_task, container, false);
            android.widget.TextView title = card.findViewById(R.id.text_task_title);
            android.widget.TextView time = card.findViewById(R.id.text_task_time);
            android.widget.CheckBox cb = card.findViewById(R.id.checkbox_done);
            android.widget.ImageView icon = card.findViewById(R.id.icon_category);

            title.setText(plan.getTitle());
            time.setText(plan.getTimeRange());
            cb.setChecked(plan.isCompleted());
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                java.util.Map<String, Object> campos = new java.util.HashMap<>();
                campos.put("estado", isChecked ? "completado" : "pendiente");
                com.utp.project.data.FirestoreService.updateActividad(plan.getId(), campos);
            });

            container.addView(card);
        }
    }

    /**
     * Carga las actividades desde Firestore y muestra las próximas actividades pendientes
     */
    private void loadActividadesFromFirestore() {
        actividadesListener = com.utp.project.data.FirestoreService.listenActividades((snap, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Error al cargar actividades: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snap == null || snap.isEmpty()) {
                // Limpiar la lista si no hay actividades
                if (actividadesList != null) {
                    actividadesList.clear();
                    if (actividadesAdapter != null) actividadesAdapter.notifyDataSetChanged();
                }
                return;
            }

            // Obtener la fecha actual para filtrar actividades futuras
            Calendar now = Calendar.getInstance();

            // Limpiar la lista
            actividadesList.clear();

            //Mapas para contar tareas totales y completadas por categoría
            java.util.Map<String, Integer> categoriaToCount = new java.util.HashMap<>();
            java.util.Map<String, Integer> categoriaToCompleted = new java.util.HashMap<>();



            // Procesar cada documento y agregar solo las actividades pendientes futuras
            for (DocumentSnapshot doc : snap.getDocuments()) {
                PlanModel plan = convertDocumentToPlanModel(doc);
                if (plan == null) continue;

                Timestamp fechaInicio = doc.getTimestamp("fechaInicio");
                String estado = doc.getString("estado");
                String prioridad = doc.getString("prioridad");
                String categoria = doc.getString("categoria");

                if (categoria == null || categoria.trim().isEmpty()) continue;

                // Contar siempre (sin importar el estado)
                categoriaToCount.put(categoria, categoriaToCount.getOrDefault(categoria, 0) + 1);

                // Si está completada, sumarla al progreso
                if ("completado".equalsIgnoreCase(estado)) {
                    categoriaToCompleted.put(categoria, categoriaToCompleted.getOrDefault(categoria, 0) + 1);
                }

                // Solo mostrar actividades pendientes y que aún no han pasado
                if (estado == null || !estado.equals("completado")) {
                    if (fechaInicio != null) {
                        Calendar fechaInicioCal = Calendar.getInstance();
                        fechaInicioCal.setTimeInMillis(fechaInicio.toDate().getTime());

                        // Solo agregar si prioridad ALTA y la fecha de inicio es en el futuro o hoy
                        boolean isHigh = prioridad != null && prioridad.equalsIgnoreCase("alta");
                        boolean isTodayOrFuture = !fechaInicioCal.before(now) ||
                                (fechaInicioCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                 fechaInicioCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR));

                        if (isHigh && isTodayOrFuture) {
                            actividadesList.add(plan);

                        }
                    }
                }
            }

            // Limitar a las próximas 10 actividades para no sobrecargar la vista
            if (actividadesList.size() > 10) {
                actividadesList = new ArrayList<>(actividadesList.subList(0, 10));
            }

            // Renderizar tarjetas MaterialCardView
            renderActividadesEnTarjetas(binding.getRoot());

            // Actualizar categorías dinámicamente (usaremos un ícono genérico)
            categoryList.clear();
            for (java.util.Map.Entry<String, Integer> entry : categoriaToCount.entrySet()) {
                String categoria = entry.getKey();
                int total = entry.getValue();
                int done = categoriaToCompleted.getOrDefault(categoria, 0);

                int icon = R.drawable.ic_work; // Puedes cambiar el ícono según la categoría si deseas
                categoryList.add(new Category(categoria, icon, total, done)); // constructor con progreso
            }
            categoryAdapter.notifyDataSetChanged();
        });
    }

    /**
     * Convierte un DocumentSnapshot de Firestore a un PlanModel
     */
    private PlanModel convertDocumentToPlanModel(DocumentSnapshot doc) {
        try {
            String titulo = doc.getString("titulo");
            if (titulo == null || titulo.isEmpty()) {
                titulo = "Sin título";
            }

            // Obtener las fechas de inicio y fin
            Timestamp fechaInicio = doc.getTimestamp("fechaInicio");
            Timestamp fechaFin = doc.getTimestamp("fechaFin");

            String timeRange = "Sin hora";
            if (fechaInicio != null && fechaFin != null) {
                timeRange = formatTimeRange(fechaInicio.toDate(), fechaFin.toDate());
            } else if (fechaInicio != null) {
                timeRange = formatTime(fechaInicio.toDate());
            }

            // Usar amarillo para actividades pendientes en Home
            int colorBar = R.color.yellow_bar;
            int colorBackground = R.color.yellow_background_alpha;

            String id = doc.getId();
            boolean completed = false;
            long startMs = fechaInicio != null ? fechaInicio.toDate().getTime() : Long.MAX_VALUE;

            return new PlanModel(id, titulo, timeRange, colorBar, colorBackground, completed, startMs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Formatea un rango de tiempo en formato "HH:mm AM/PM - HH:mm AM/PM"
     */
    private String formatTimeRange(java.util.Date inicio, java.util.Date fin) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        return sdf.format(inicio) + " - " + sdf.format(fin);
    }

    /**
     * Formatea una fecha en formato "HH:mm AM/PM"
     */
    private String formatTime(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        return sdf.format(date);
    }
}
