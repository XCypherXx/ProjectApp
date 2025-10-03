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

    // Variable para manejar callbacks del SDK de Facebook
    private CallbackManager mCallbackManager;

    // Declaración de variables
    private EditText editTextUsuario, editTextPassword, editTextConfirmPassword;
    private MaterialButton btnRegistrarse, btnGoogle, btnFacebook;
    private CheckBox cbAceptarTerminos;
    private TextInputLayout textInputLayoutPassword, textInputLayoutConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicialización de Firebase
        mAuth = FirebaseAuth.getInstance();

        // [COMPROBACIÓN DE SESIÓN] Si el usuario ya está logueado, saltar a la siguiente pantalla
        if (mAuth.getCurrentUser() != null) {
            openNextScreen();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Inicialización del SDK de Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        mCallbackManager = CallbackManager.Factory.create();

        // Configuración de Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Obtener referencias de la UI
        editTextUsuario = findViewById(R.id.editText_usuario);
        editTextPassword = findViewById(R.id.editText_password);
        editTextConfirmPassword = findViewById(R.id.editText_confirmPassword);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        cbAceptarTerminos = findViewById(R.id.cbAceptarTerminos);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);

        // Botones Sociales
        btnGoogle = findViewById(R.id.btnGoogle);
        // Nota: Asegúrate que tu activity_registro.xml tenga el id btnFacebook
        btnFacebook = findViewById(R.id.btnFacebook);

        // Listener para CheckBox de Términos y Condiciones
        cbAceptarTerminos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnRegistrarse.setEnabled(isChecked);
                if (isChecked) {
                    btnRegistrarse.setAlpha(1.0f); // Opacidad normal
                } else {
                    btnRegistrarse.setAlpha(0.5f); // Opacidad reducida
                }
            }
        });

        // Listener del botón Registrarse (Email/Password)
        btnRegistrarse.setOnClickListener(view -> registrarUsuario());

        // Listener para botón de Google
        btnGoogle.setOnClickListener(view -> signIn());

        // Listener para el botón de Facebook
        btnFacebook.setOnClickListener(view -> loginWithFacebook());

        // Registro del Callback para Facebook
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
    }

    private void registrarUsuario() {
        String email = editTextUsuario.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ActivityRegistro.this, "Registro exitoso con Email", Toast.LENGTH_SHORT).show();
                        // Llama a la función de navegación única
                        openNextScreen();
                    } else {
                        Toast.makeText(ActivityRegistro.this, "Fallo el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Sobreescribir onActivityResult para manejar tanto Google como Facebook
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

    // Inicio de sesión con Google
    private void signIn() {
        // test (opcional: limpiar sesión anterior de Google antes de iniciar)
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
                        // Llama a la función de navegación única
                        openNextScreen();
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
                        // Llama a la función de navegación única
                        openNextScreen();
                    } else {
                        Log.e("FacebookAuth", "Falló la autenticación de Firebase con Facebook", task.getException());
                        Toast.makeText(this, "Falló la autenticación de Firebase con Facebook: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openNextScreen() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        // Lee si es la primera vez, el valor por defecto es 'true'
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        if (isFirstTime) {
            // Si es la primera vez, marcamos que ya vio el Onboarding
            prefs.edit().putBoolean("isFirstTime", false).apply();
            // Abrir Onboarding
            Intent intent = new Intent(ActivityRegistro.this, OnboardingActivity.class);
            startActivity(intent);
        } else {
            // Si ya no es la primera vez, ir directo al Home
            Intent intent = new Intent(ActivityRegistro.this, HomeActivity.class);
            startActivity(intent);
        }
        finish();
    }
}