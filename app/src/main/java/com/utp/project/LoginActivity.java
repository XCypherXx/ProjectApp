package com.utp.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Necesario para Log de Facebook/Firebase
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider; // [FACEBOOK]
import java.util.Arrays; // [FACEBOOK]

// [FACEBOOK] Importaciones del SDK
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.appevents.AppEventsLogger;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // [FACEBOOK] Variable para manejar callbacks del SDK de Facebook
    private CallbackManager mCallbackManager;

    // Variables existentes del login clásico
    private EditText editTextUsuario;
    private EditText editTextPassword;
    private MaterialButton buttonIngresar;

    // [SOCIAL] Declaración de botones
    private MaterialButton btnGoogle, btnFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicialización de Firebase (Necesaria antes de la comprobación)
        mAuth = FirebaseAuth.getInstance();

        // [COMPROBACIÓN DE SESIÓN] Si ya está logueado, ir a Home
        if (mAuth.getCurrentUser() != null) {
            openHomeActivity();
            return; // Detener la ejecución de onCreate()
        }

        setContentView(R.layout.activity_login);

        // [FACEBOOK] Inicialización del SDK y CallbackManager
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        mCallbackManager = CallbackManager.Factory.create();

        // Configuración de Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        // Obtener referencias de los elementos del layout (Login Clásico)
        editTextUsuario = findViewById(R.id.editText_usuario);
        editTextPassword = findViewById(R.id.editText_password);
        buttonIngresar = findViewById(R.id.button_ingresar);

        // [SOCIAL] Obtener referencias de botones sociales
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);

        // Listener del botón Ingresar (Login FireBase)
        buttonIngresar.setOnClickListener(v -> {
            // Login anónimo en Firebase (para pruebas)
            mAuth.signInAnonymously()
                    .addOnSuccessListener(r -> {
                        // Crea/merge doc de usuario
                        com.utp.project.data.FirestoreService.ensureUserDocument(null)
                                .addOnCompleteListener(x -> openHomeActivity());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(LoginActivity.this, "Error autenticación anónima: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        // [GOOGLE] Listener para botón de Google
        btnGoogle.setOnClickListener(view -> signInWithGoogle());

        // [FACEBOOK] Listener para el botón de Facebook
        btnFacebook.setOnClickListener(view -> loginWithFacebook());

        // [FACEBOOK] Registro del Callback para Facebook
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("FacebookAuth", "Facebook login exitoso. Token: " + loginResult.getAccessToken().getToken());
                handleFacebookAccessToken(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Inicio de sesión con Facebook cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("FacebookAuth", "Error en login de Facebook", error);
                Toast.makeText(LoginActivity.this, "Error de Facebook: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // MANEJO DE RESULTADOS DE ACTIVIDAD (Google y Facebook)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1. Pasa el resultado a CallbackManager de Facebook.
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        // 2. Maneja el resultado de Google
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

    // MÉTODOS DE GOOGLE
    private void signInWithGoogle() {
        // test - importante para permitir elegir cuenta cada vez
        mGoogleSignInClient.signOut();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Inicio de sesión exitoso con Google: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        // NUEVO: crear/merge doc de usuario en Firestore
                        com.utp.project.data.FirestoreService.ensureUserDocument(null)
                                .addOnCompleteListener(x -> openHomeActivity());
                    } else {
                        Toast.makeText(this, "Falló el inicio de sesión con Google: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // MÉTODOS DE FACEBOOK
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
                        // NUEVO: crear/merge doc de usuario en Firestore
                        com.utp.project.data.FirestoreService.ensureUserDocument(null)
                                .addOnCompleteListener(x -> openHomeActivity());
                    } else {
                        Log.e("FacebookAuth", "Falló la autenticación de Firebase con Facebook", task.getException());
                        Toast.makeText(this, "Falló la autenticación de Firebase con Facebook: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // NAVEGACIÓN
    private void openHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Cierra la actividad de login
    }
}