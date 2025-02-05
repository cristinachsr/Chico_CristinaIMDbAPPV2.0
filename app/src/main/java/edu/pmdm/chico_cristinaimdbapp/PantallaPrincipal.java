package edu.pmdm.chico_cristinaimdbapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;


public class PantallaPrincipal extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private CallbackManager callbackManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bloquear la orientación de la pantalla en modo vertical
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Configurar layout de la actividad principal
        setContentView(R.layout.pantallaprincipal);


        // 🔥 Verificar primero en la base de datos si el usuario existe
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String userId = sharedPreferences.getString("userId", null);
        String authMethod = sharedPreferences.getString("authMethod", null); // 🔥 Obtener el método de autenticación

        if (userId != null) {
            // Verificar si el usuario existe en la base de datos local
            if (!favoritesManager.isUserExists(userId)) {
                Log.e("Sesion", "⚠ Usuario autenticado pero no encontrado en la base local. Sincronizando...");

                // Obtener datos desde Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db.collection("users").document(userId);

                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String emailFromDB = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        // Validar y asignar valores predeterminados si faltan datos
                        if (name == null) name = "Usuario";
                        if (photoUrl == null) photoUrl = "android.resource://" + getPackageName() + "/drawable/logoandroid";

                        // Guardar en la base local
                        favoritesManager.addOrUpdateUser(userId, name, emailFromDB, null, null, null, null, photoUrl);

                        // Redirigir al MainActivity
                        Log.d("Sesion", "🟢 Usuario sincronizado con éxito. Redirigiendo...");
                        goToMainActivity();
                    } else {
                        Log.e("Sesion", "⚠ Usuario autenticado pero no encontrado en Firestore.");
                    }
                }).addOnFailureListener(e -> Log.e("Firestore", "Error al obtener datos del usuario.", e));
            } else {
                Log.d("Sesion", "✅ Usuario encontrado en la base local. Redirigiendo...");
                goToMainActivity();
            }
        }

            // Configurar opciones de inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Referencia al botón Sign-In de Google
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        setSignInButtonText(signInButton);
        signInButton.setOnClickListener(v -> signIn());


        mAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();


        Button facebookLoginButton = findViewById(R.id.facebook_login_button);
        facebookLoginButton.setOnClickListener(view -> loginWithFacebook());

        //registro gmail
        //GMAIL
       EditText emailField = findViewById(R.id.emailField);
        EditText passwordField = findViewById(R.id.passwordField);
        Button registerButton = findViewById(R.id.registerButton);
        Button loginButton = findViewById(R.id.loginButton);

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            checkEmailSignInMethod(email,
                    () -> registerWithEmail(email, password), // Si el correo es válido para registrarse
                    () -> Toast.makeText(this, "Este correo ya está registrado con otro método. Usa Google Sign-In.", Toast.LENGTH_LONG).show()
            );
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim(); // Trim para evitar espacios vacíos

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            loginWithEmail(email, password);
        });


    }

    private void checkEmailSignInMethod(String email, Runnable onEmailAvailable, Runnable onEmailUsedWithOtherMethod) {
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null && !signInMethods.isEmpty()) {
                            if (signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                                // 🔹 El correo ya está registrado con Email/Password, permitir registro
                                onEmailAvailable.run();
                            } else {
                                // 🔹 El correo ya está registrado con otro método (Google/Facebook)
                                onEmailUsedWithOtherMethod.run();
                            }
                        } else {
                            // 🔹 El correo no está registrado en Firebase
                            onEmailAvailable.run();
                        }
                    } else {
                        Log.e("FirebaseAuth", "Error al comprobar el método de inicio de sesión.", task.getException());
                    }
                });
    }



    private void registerWithEmail(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, introduce un correo válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el correo está registrado con Google Sign-In
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null && signInMethods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD)) {
                            Toast.makeText(this, "Este correo ya está registrado con Google Sign-In. Usa ese método para iniciar sesión.", Toast.LENGTH_LONG).show();
                        } else {
                            // Proceder con el registro si no está registrado con Google Sign-In
                            completeEmailRegistration(email, password);
                        }
                    } else {
                        Log.e("FirebaseAuth", "Error al verificar el método de inicio de sesión.", task.getException());
                        Toast.makeText(this, "Error al verificar el correo. Inténtalo nuevamente.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void completeEmailRegistration(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            String name = "Usuario"; // Nombre por defecto
                            String photoUrl = null; // No usar imagen predeterminada

                            // 🔥 GUARDAR EN FIRESTORE
                            saveUserToFirestore(userId, name, email, photoUrl);

                            // 🔥 GUARDAR EN SQLite
                            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                            favoritesManager.addOrUpdateUser(userId, name, email, null, null, null, null, photoUrl);

                            // 🔥 GUARDAR EN SharedPreferences
                            saveUserToSharedPreferences(userId, name, email, photoUrl, "email");

                            // 🔥 REDIRECCIONAR AL MAIN
                            Toast.makeText(this, "Registro exitoso. Bienvenido/a.", Toast.LENGTH_SHORT).show();
                            goToMainActivityWithEmail(userId, name, email);
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }






    private void loginWithEmail(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, introduce un correo válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            String emailFromFirebase = user.getEmail();

                            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);

                            if (!favoritesManager.isUserExists(userId)) {
                                Log.e("Sync", "⚠ Usuario no encontrado en SQLite. Cargando desde Firestore...");

                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                DocumentReference userRef = db.collection("users").document(userId);

                                userRef.get().addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String name = documentSnapshot.getString("name");
                                        String photoUrl = documentSnapshot.getString("photoUrl");

                                        if (name == null) name = "Usuario";
                                        if (photoUrl == null) photoUrl = null; // No usar imagen predeterminada

                                        // 🔥 GUARDAR EN SQLite
                                        favoritesManager.addOrUpdateUser(userId, name, emailFromFirebase, null, null, null, null, photoUrl);

                                        // 🔥 GUARDAR EN SharedPreferences
                                        saveUserToSharedPreferences(userId, name, emailFromFirebase, photoUrl, "email");

                                        Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                                        goToMainActivityWithEmail(userId, name, emailFromFirebase);
                                    } else {
                                        Log.e("Firestore", "⚠ Usuario no encontrado en Firestore.");
                                        Toast.makeText(this, "Error: No se encontró el usuario en la base de datos.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                goToMainActivityWithEmail(userId, "Usuario", emailFromFirebase);
                            }
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }





    private void handleAuthError(Exception exception) {
        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                // Contraseña incorrecta
                Toast.makeText(this, "Contraseña incorrecta. Intenta nuevamente.", Toast.LENGTH_SHORT).show();
            } else if (exception instanceof FirebaseAuthInvalidUserException) {
                // Usuario no encontrado
                Toast.makeText(this, "No existe una cuenta con este correo. Regístrate primero.", Toast.LENGTH_SHORT).show();
            } else if (exception instanceof FirebaseAuthUserCollisionException) {
                // Correo ya registrado con otro método
                Toast.makeText(this, "Este correo ya está registrado. Usa Google Sign-In o inicia sesión.", Toast.LENGTH_LONG).show();
            } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                // Contraseña débil
                Toast.makeText(this, "La contraseña es demasiado débil. Usa al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            } else if (exception instanceof FirebaseAuthEmailException) {
                // Error relacionado con el correo electrónico
                Toast.makeText(this, "Error con el correo electrónico. Verifica el formato.", Toast.LENGTH_SHORT).show();
            } else {
                // Otros errores
                Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveUserToFirestore(String userId, String name, String email, String photoUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("photoUrl", photoUrl);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado en Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario en Firestore", e));
    }

    private void saveUserToSharedPreferences(String userId, String name, String email, String photoUrl, String authMethod) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", userId);
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("photoUrl", photoUrl);
        editor.putString("authMethod", authMethod); // Método de autenticación
        editor.apply();
    }
    private void goToMainActivityWithEmail(String userId, String name, String email) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("name", name != null ? name : "Usuario"); // Predeterminado si es null
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }




    //FACEBOOK
    private void loginWithFacebook() {
        // Solicitar permisos
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));

        // Registrar el callback para manejar el resultado del inicio de sesión
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Manejar el token de acceso exitoso
                handleFacebookAccessToken(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(PantallaPrincipal.this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(PantallaPrincipal.this, "Error en inicio de sesión", Toast.LENGTH_SHORT).show();
                Log.e("FacebookLogin", "Error en el login", error);
            }
        });
    }
    private void handleFacebookAccessToken(String token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();

                        com.facebook.GraphRequest request = com.facebook.GraphRequest.newMeRequest(
                                accessToken, (object, response) -> {
                                    try {
                                        String userId = object.getString("id");
                                        String name = object.getString("name");
                                        String email = object.has("email") ? object.getString("email") : "Correo no disponible";
                                        String photoUrl = "https://graph.facebook.com/" + userId + "/picture?type=large&access_token=" + accessToken.getToken();

                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        DocumentReference userRef = db.collection("users").document(userId);

                                        userRef.get().addOnSuccessListener(documentSnapshot -> {
                                            if (!documentSnapshot.exists()) {
                                                // 🔥 Usuario NO registrado en Firestore, guardarlo
                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("userId", userId);
                                                userData.put("name", name);
                                                userData.put("email", email);
                                                userData.put("photoUrl", photoUrl);
                                                userData.put("authMethod", "facebook"); // 🔥 Guardar método de autenticación

                                                userRef.set(userData)
                                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado en Firestore"))
                                                        .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario", e));
                                            } else {
                                                Log.d("Firestore", "Usuario ya registrado en Firestore, sincronizando favoritos...");
                                            }

                                            // Guardar datos en SharedPreferences
                                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString("userId", userId);
                                            editor.putString("name", name);
                                            editor.putString("email", email);
                                            editor.putString("photoUrl", photoUrl);
                                            editor.putString("authMethod", "facebook"); // 🔥 Guardar método de autenticación
                                            editor.apply();

                                            // 🔥 Sincronizar favoritos
                                            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                                            favoritesManager.syncFavorites(userId);


                                            // **🔥 Guardar en la base de datos local usuario(SQLite)**
                                            favoritesManager.addOrUpdateUser(userId, name, email, null, null, null, null, photoUrl);

                                            // Escuchar cambios en tiempo real
                                            favoritesManager.listenForFavoriteChanges(userId);


                                            // Redirigir a MainActivity
                                            goToMainActivityFacebook();
                                        });

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email");
                        request.setParameters(parameters);
                        request.executeAsync();

                    } else {
                        Toast.makeText(PantallaPrincipal.this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void goToMainActivityFacebook() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        String name = sharedPreferences.getString("name", "Usuario");
        String email = sharedPreferences.getString("email", "Correo no disponible");
        String authMethod = sharedPreferences.getString("authMethod", null);

        if (userId != null && "facebook".equals(authMethod)) {
            Log.d("FacebookSignIn", "Redirigiendo a MainActivity desde Facebook: " + email);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        } else {
            Log.e("FacebookSignIn", "No se pudo obtener la cuenta de Facebook.");
            Toast.makeText(this, "Error al obtener datos de Facebook", Toast.LENGTH_SHORT).show();
        }
    }









    //GOOGLE
    // Configurar el texto del botón de inicio de sesión con Google
    private void setSignInButtonText(SignInButton signInButton) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setText("Sign in with Google");
                break;
            }
        }
    }

    // Método para iniciar sesión con Google
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    // Manejar el resultado del inicio de sesión con Google
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleSignInResult(task);
                        } else {
                            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String userId = account.getId();
                String name = account.getDisplayName();
                String email = account.getEmail();
                Uri photoUri = account.getPhotoUrl();
                String photoUrl = (photoUri != null) ? photoUri.toString() : null;

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db.collection("users").document(userId);

                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Usuario NO registrado en Firestore, guardarlo
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", userId);
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("photoUrl", photoUrl);

                        userRef.set(userData)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado en Firestore"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario", e));
                    } else {
                        Log.d("Firestore", "Usuario ya registrado en Firestore.");
                    }

                    // Guardar datos en SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("userId", userId);
                    editor.putString("name", name);
                    editor.putString("email", email);
                    editor.putString("photoUrl", photoUrl);
                    editor.putString("authMethod", "google"); // 🔥 Registrar el método de autenticación
                    editor.apply();

                    // Sincronizar favoritos y usuario
                    FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                    favoritesManager.syncFavorites(userId);
                    favoritesManager.addOrUpdateUser(userId, name, email, null, null, null, null, photoUrl);


                    // Redirigir a MainActivity
                    goToMainActivity();
                });
            }
        } catch (ApiException e) {
            Log.w("PantallaPrincipal", "Error en inicio de sesión: " + e.getStatusCode());
            Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
        }
    }
    private void goToMainActivity() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d("GoogleSignIn", "Redirigiendo a MainActivity: " + account.getEmail());
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("name", account.getDisplayName());
            intent.putExtra("email", account.getEmail());
            intent.putExtra("userId", account.getId());
            startActivity(intent);
            finish();
        } else {
            Log.e("GoogleSignIn", "No se pudo obtener la cuenta de Google.");
            Toast.makeText(this, "Error al obtener datos de Google", Toast.LENGTH_SHORT).show();
        }
    }


}
