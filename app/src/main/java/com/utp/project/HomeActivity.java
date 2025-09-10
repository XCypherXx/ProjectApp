package com.utp.project;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ArrayList<Category> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        TextView userGreeting = findViewById(R.id.user_greeting);
        ImageView profileImage = findViewById(R.id.profile_image);

        if (user != null) {
            // Mostrar nombre
            String name = user.getDisplayName();
            userGreeting.setText("Hola, " + name);

            // Mostrar foto (si tiene)
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .into(profileImage);
            }
        }


        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);

        // Lista de ejemplo
        categoryList = new ArrayList<>();
        categoryList.add(new Category("Work", R.drawable.ic_work, 2)); //  (Nombre, Icono, Tareas)
        categoryList.add(new Category("Personal", R.drawable.ic_school, 1));
        categoryList.add(new Category("Shopping", R.drawable.ic_home, 3));
        categoryList.add(new Category("Health", R.drawable.ic_health, 0));

        categoryAdapter = new CategoryAdapter(this, categoryList);
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Layout horizontal slider de cards
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        categoryRecyclerView.setLayoutManager(layoutManager);

        // Clicks en las cards
        categoryAdapter.setOnItemClickListener(position -> {
            Category clicked = categoryList.get(position);
            Toast.makeText(this, "Clicked: " + clicked.getName(), Toast.LENGTH_SHORT).show();
        });
    }
}