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


public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    // private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ArrayList<Category> categoryList;

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
            // Mostrar nombre
            String name = user.getDisplayName();
            binding.userGreeting.setText("Hola, " + name);

            // Mostrar foto (si tiene)
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .into(binding.profileImage);
            }
        } else {
            // Caso 2: Usuario registrado localmente
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUser = prefs.getString("currentUser", null);
            binding.userGreeting.setText("Hola, " + currentUser + " 👋");
            //prueba por default
            binding.profileImage.setImageResource(R.drawable.ic_user_default);
        }


        // categoryRecyclerView = findViewById(R.id.categoryRecyclerView);

        // Lista de ejemplo
        categoryList = new ArrayList<>();
        categoryList.add(new Category("Work", R.drawable.ic_work, 2)); //  (Nombre, Icono, Tareas)
        categoryList.add(new Category("Personal", R.drawable.ic_school, 1));
        categoryList.add(new Category("Shopping", R.drawable.ic_home, 3));
        categoryList.add(new Category("Health", R.drawable.ic_health, 0));

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
        return view;


    }

}
