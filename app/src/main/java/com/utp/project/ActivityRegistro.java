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

import com.google.firebase.auth.FacebookAuthProvider;
import java.util.Arrays;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.appevents.AppEventsLogger;
import android.util.Log;

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
    private CallbackManager mCallbackManager;
    private MaterialButton btnFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // [FACEBOOK] Inicialización del SDK y CallbackManager
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        mCallbackManager = CallbackManager.Factory.create();

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

        // Botones Sociales
        btnFacebook = findViewById(R.id.btnFacebook);

        btnRegistrarse.setOnClickListener(v -> {
            String username = editTextUsuario.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

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

            // Guardar nombre en SharedPreferences para mostrar en Home
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("username", username)
                    .putString("password", password)
                    .putString("currentUser", username)
                    .apply();

            // NUEVO: Autenticación anónima en Firebase y luego actualizar nombre
            mAuth.signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        // Actualizar displayName del usuario
                        com.google.firebase.auth.UserProfileChangeRequest req =
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();
                        mAuth.getCurrentUser().updateProfile(req)
                                .addOnCompleteListener(task -> {
                                    // Guardar en Firestore con nombre en perfil
                                    java.util.Map<String, Object> extra = new java.util.HashMap<>();
                                    java.util.Map<String, Object> perfil = new java.util.HashMap<>();
                                    perfil.put("nombre", username);
                                    extra.put("perfil", perfil);

                                    com.utp.project.data.FirestoreService.ensureUserDocument(extra)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                                // Abrir onboarding (ya sabemos que es primera vez)
                                                openNextScreen(mAuth.getCurrentUser().getUid());
                                            });
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al registrarse: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
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

        // [FACEBOOK] Listener para el botón de Facebook
        btnFacebook.setOnClickListener(view -> loginWithFacebook());

        // [FACEBOOK] Registro del Callback para Facebook
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Si el login de FB es exitoso, pasa el token a Firebase
                Log.d("FacebookAuth", "Facebook login exitoso. Token: " + loginResult.getAccessToken().getToken());
                handleFacebookAccessToken(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(ActivityRegistro.this, "Inicio de sesión con Facebook cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("FacebookAuth", "Error en login de Facebook", error);
                Toast.makeText(ActivityRegistro.this, "Error de Facebook: " + error.getMessage(), Toast.LENGTH_LONG).show();
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

    private void loginWithFacebook() {
        // Pedir permisos de email y perfil público
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(String token) {
        // Usa el token de Facebook para obtener una credencial de Firebase
        AuthCredential credential = FacebookAuthProvider.getCredential(token);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Inicio de sesión exitoso con Facebook: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                        if (mAuth.getCurrentUser() != null) {
                            openNextScreen( user.getUid());
                            return;
                        }
                    } else {
                        Log.e("FacebookAuth", "Falló la autenticación de Firebase con Facebook", task.getException());
                        Toast.makeText(this, "Falló la autenticación de Firebase con Facebook: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1. Pasa el resultado a CallbackManager de Facebook. (NUEVO)
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        // 2. Maneja el resultado de Google (EXISTENTE)
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
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
        String key = "isFirstTime_" + userId;

        boolean isFirstTime = prefs.getBoolean(key, true);
        if (isFirstTime) {
            // Aún no vio onboarding → ir a onboarding sin marcarla aquí
            Intent intent = new Intent(ActivityRegistro.this, OnboardingActivity.class);
            startActivity(intent);
        } else {
            // Ya vio onboarding → ir directo a Home
            Intent intent = new Intent(ActivityRegistro.this, HomeActivity.class);
            startActivity(intent);
        }
        finish();
    }
}