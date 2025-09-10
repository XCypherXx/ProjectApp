package com.utp.project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;

import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

public class ActivityRegistro extends AppCompatActivity {


    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    // fb private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // fb    mCallbackManager = CallbackManager.Factory.create();

/*
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("FACEBOOK_AUTH", "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d("FACEBOOK_AUTH", "facebook:onCancel");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e("FACEBOOK_AUTH", "facebook:onError", error);
                    }
                });
       //btn fb
        MaterialButton btnFacebookCustom = findViewById(R.id.btnFacebookCustom);
        btnFacebookCustom.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    Arrays.asList("email", "public_profile")
            );
        });
        */

        //btn google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // el token que te da Firebase
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


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
        // fb     mCallbackManager.onActivityResult(requestCode, resultCode, data);

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
                        if (user != null) {
                            Log.d("FIREBASE_AUTH", "Login exitoso. Usuario: " + user.getEmail());
                            Log.d("FIREBASE_AUTH", "DisplayName: " + user.getDisplayName());
                            Log.d("FIREBASE_AUTH", "PhotoUrl: " + user.getPhotoUrl());

                            Toast.makeText(this, "Inicio de sesión exitoso: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            Log.e("FIREBASE_AUTH", "El login fue exitoso, pero FirebaseUser es null");
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("FIREBASE_AUTH", "Falló el inicio de sesión", e);
                        Toast.makeText(this, "Falló el inicio de sesión: " + (e != null ? e.getMessage() : "Error desconocido"), Toast.LENGTH_SHORT).show();
                    }
                });


    }
/*
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("FACEBOOK_AUTH", "Login exitoso con Facebook: " + user.getEmail());
                            Toast.makeText(this, "Bienvenido: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        }
                    } else {
                        Log.e("FACEBOOK_AUTH", "Falló login con Facebook", task.getException());
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
*/

}