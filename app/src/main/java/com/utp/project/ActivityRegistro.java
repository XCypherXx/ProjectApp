package com.utp.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class ActivityRegistro extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Declaración de variables
    private EditText editTextUsuario, editTextPassword, editTextConfirmPassword;
    private MaterialButton btnRegistrarse;
    private CheckBox cbAceptarTerminos;

    // Referencias a los TextInputLayout
    private TextInputLayout textInputLayoutPassword;
    private TextInputLayout textInputLayoutConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut(); //prueba
        // Obtener referencias de los elementos del layout
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        cbAceptarTerminos = findViewById(R.id.cbAceptarTerminos);
        // Referencias correctas
        editTextUsuario = findViewById(R.id.editText_usuario);
        editTextPassword = findViewById(R.id.editText_password);
        editTextConfirmPassword = findViewById(R.id.editText_confirmPassword);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);

        // Referencias a los TextInputLayout para manejo de errores
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);

        btnRegistrarse.setOnClickListener(v -> {
            String username = editTextUsuario.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            // Limpiar errores anteriores
            textInputLayoutPassword.setError(null);
            textInputLayoutConfirmPassword.setError(null);

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                textInputLayoutConfirmPassword.setError("Las contraseñas no coinciden");
                return;
            }

            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("username", username)
                    .putString("password", password)
                    .putString("currentUser", username)
                    // .putBoolean("isFirstTime", true)
                    .apply();

            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
            openNextScreen(username);
        });

        // Deshabilitar el botón de registro por defecto
        btnRegistrarse.setEnabled(false);

        // Agregar listener al CheckBox para habilitar/deshabilitar el botón
        cbAceptarTerminos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnRegistrarse.setEnabled(isChecked);
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // el token que te da Firebase
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        //prueba
        mGoogleSignInClient.signOut();
        // btn de Google
        MaterialButton btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Error en Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Inicio de sesión exitoso: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        // Si ya hay sesión activa, ir a home desde el btn

                        if (mAuth.getCurrentUser() != null) {
                            openNextScreen(user.getUid()); // manejar onboarding o home
                            return;
                        }
                    } else {
                        Toast.makeText(this, "Falló el inicio de sesión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openNextScreen(String userId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
    // una sola bandera
        //boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        //   para el onboarding  clave única por usuario
        String key = "isFirstTime_" + userId;

        boolean isFirstTime = prefs.getBoolean(key, true);
        if (isFirstTime) {
            // Guardar que ya vio onboarding
            prefs.edit().putBoolean("isFirstTime", false).apply();
            // Abrir onboarding
            Intent intent = new Intent(ActivityRegistro.this, OnboardingActivity.class);
            startActivity(intent);
        } else {
            // Abrir Home directamente
            Intent intent = new Intent(ActivityRegistro.this, HomeActivity.class);
            startActivity(intent);
        }
        finish();
    }
}