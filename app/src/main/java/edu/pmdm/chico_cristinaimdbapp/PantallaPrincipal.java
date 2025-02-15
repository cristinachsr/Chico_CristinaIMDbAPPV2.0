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


        //  Verificar primero en la base de datos si el usuario existe
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String userId = sharedPreferences.getString("userId", null);
        String authMethod = sharedPreferences.getString("authMethod", null); //  Obtener el método de autenticación

        if (userId != null) {
            // Si el usuario ya está en la base de datos local, redirigir directamente
            if (favoritesManager.isUserExists(userId)) {
                Log.d("Sesion", " Usuario encontrado en la base local. Redirigiendo...");

                // Redirigir según el método de autenticación
                if ("email".equals(authMethod)) {
                    goToMainActivityWithEmail(userId, sharedPreferences.getString("name", "Usuario"),
                            sharedPreferences.getString("email", "Correo no disponible"));
                } else if ("facebook".equals(authMethod)) {
                    goToMainActivityFacebook();
                } else {
                    goToMainActivity(); // Por defecto, Google
                }
                return;
            }

            //  Si el usuario no está en la base local, buscar en Firestore
            Log.e("Sesion", " Usuario autenticado pero no encontrado en la base local. Sincronizando...");

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    String emailFromDB = documentSnapshot.getString("email");
                    String photoUrl = documentSnapshot.getString("image");
                    String authMethodFromDB = documentSnapshot.getString("authMethod"); //  Obtener método de autenticación

                    //  Validar valores predeterminados si faltan datos
                    if (name == null) name = "Usuario";
                    if (photoUrl == null || photoUrl.isEmpty()) {
                        photoUrl = "google".equals(authMethodFromDB)
                                ? "https://ui-avatars.com/api/?name=" + name + "&background=0D8ABC&color=fff"
                                : "android.resource://" + getPackageName() + "/drawable/logoandroid";
                    }

                    //  Guardar en la base local
                    favoritesManager.addOrUpdateUser(userId, name, emailFromDB, null, null, null, null, photoUrl);

                    //  Guardar en SharedPreferences para futuras sesiones
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("name", name);
                    editor.putString("email", emailFromDB);
                    editor.putString("image", photoUrl);
                    editor.putString("authMethod", authMethodFromDB);
                    editor.apply();

                    //  Redirigir según el método de autenticación
                    Log.d("Sesion", " Usuario sincronizado con éxito. Redirigiendo...");
                    if ("email".equals(authMethodFromDB)) {
                        goToMainActivityWithEmail(userId, name, emailFromDB);
                    } else if ("facebook".equals(authMethodFromDB)) {
                        goToMainActivityFacebook();
                    } else {
                        goToMainActivity(); // Por defecto, Google
                    }
                } else {
                    Log.e("Sesion", " Usuario autenticado pero no encontrado en Firestore.");
                }
            }).addOnFailureListener(e -> Log.e("Firestore", " Error al obtener datos del usuario.", e));
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


    //verificar el correo
    private void checkEmailSignInMethod(String email, Runnable onEmailAvailable, Runnable onEmailUsedWithOtherMethod) {
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null && !signInMethods.isEmpty()) {
                            if (signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                                //  El correo ya está registrado con Email/Password, permitir registro
                                onEmailAvailable.run();
                            } else {
                                // El correo ya está registrado con otro método (Google/Facebook)
                                onEmailUsedWithOtherMethod.run();
                            }
                        } else {
                            //  El correo no está registrado en Firebase
                            onEmailAvailable.run();
                        }
                    } else {
                        Log.e("FirebaseAuth", "Error al comprobar el método de inicio de sesión.", task.getException());
                    }
                });
    }
    //registro correo y contraseña
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
    //para compeltar el metodo de registro
    private void completeEmailRegistration(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            String name = "Usuario";
                            String image = "android.resource://" + getPackageName() + "/drawable/logoandroid"; // Imagen por defecto

                            //  GUARDAR EN FIRESTORE
                            saveUserToFirestore(userId, name, email, image);

                            //  GUARDAR EN SQLite
                            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                            favoritesManager.addOrUpdateUser(userId, name, email, null, null, null, null, image);

                            //  VINCULAR AL USUARIO CON SU LISTA DE FAVORITOS
                            favoritesManager.syncFavorites(userId);

                            //  GUARDAR EN SharedPreferences
                            saveUserToSharedPreferences(userId, name, email, image, "email");

                            //  REDIRECCIONAR AL MAIN
                            Toast.makeText(this, "Registro exitoso. Bienvenido/a.", Toast.LENGTH_SHORT).show();
                            goToMainActivityWithEmail(userId, name, email);
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }
    //metodo acceder si ya han sido registrado
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
                                Log.e("Sync", " Usuario no encontrado en SQLite. Cargando desde Firestore...");

                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                DocumentReference userRef = db.collection("users").document(userId);

                                userRef.get().addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String name = documentSnapshot.getString("name");
                                        String image = documentSnapshot.getString("image");

                                        if (name == null) name = "Usuario";
                                        if (image == null) image = "android.resource://" + getPackageName() + "/drawable/logoandroid";

                                        //  GUARDAR EN SQLite
                                        favoritesManager.addOrUpdateUser(userId, name, emailFromFirebase, null, null, null, null, image);

                                        // VINCULAR FAVORITOS
                                        favoritesManager.syncFavorites(userId);

                                        //  GUARDAR EN SharedPreferences
                                        saveUserToSharedPreferences(userId, name, emailFromFirebase, image, "email");

                                        Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                                        goToMainActivityWithEmail(userId, name, emailFromFirebase);
                                    } else {
                                        Log.e("Firestore", " Usuario no encontrado en Firestore.");
                                        Toast.makeText(this, "Error: No se encontró el usuario en la base de datos.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                //  Si el usuario ya está en SQLite, sincronizar sus favoritos
                                favoritesManager.syncFavorites(userId);

                                //  Si el usuario ya está en SQLite, guardar en SharedPreferences
                                saveUserToSharedPreferences(userId, "Usuario", emailFromFirebase, null, "email");

                                goToMainActivityWithEmail(userId, "Usuario", emailFromFirebase);
                            }
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }
    //metodo para comprobar errores
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
    //guardar datos usario en firestore
    private void saveUserToFirestore(String userId, String name, String email, String image) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("image", image); // Asegurar que el nombre de la columna coincide

        if (image != null) {
            userData.put("image", image);
        } else {
            userData.put("image", "android.resource://" + getPackageName() + "/drawable/logoandroid"); // Imagen por defecto
        }

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado en Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario en Firestore", e));
    }
    //guardar datos usario en sharedpreferences
    private void saveUserToSharedPreferences(String userId, String name, String email, String image, String authMethod) {
        if (image == null || image.isEmpty()) {
            image = "android.resource://" + getPackageName() + "/drawable/logoandroid"; // Imagen predeterminada
        }
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", userId);
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("image", image); // Cambiar de photoUrl a image
        editor.putString("authMethod", authMethod);
        editor.apply();
    }
    //metodo ir mainactivity
    private void goToMainActivityWithEmail(String userId, String name, String email) {
        Log.d("Navigation", "Redirigiendo a MainActivity con: " + userId + ", " + name + ", " + email);

        if (userId == null || email == null) {
            Log.e("Navigation", " Error: userId o email son nulos. No se puede redirigir.");
            Toast.makeText(this, "Error al obtener datos del usuario.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("name", name != null ? name : "Usuario");
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
                                                //  Usuario NO registrado en Firestore, guardarlo
                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("userId", userId);
                                                userData.put("name", name);
                                                userData.put("email", email);
                                                userData.put("photoUrl", photoUrl);
                                                userData.put("authMethod", "facebook"); //  Guardar método de autenticación

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
                                            editor.putString("authMethod", "facebook"); //  Guardar método de autenticación
                                            editor.apply();

                                            //  Sincronizar favoritos
                                            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                                            favoritesManager.syncFavorites(userId);


                                            // ** Guardar en la base de datos local usuario(SQLite)**
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
    //metodo ir pantalla de mainactivity
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
                String image = (photoUri != null) ? photoUri.toString() : "https://ui-avatars.com/api/?name=" + name.replace(" ", "%20");

                // Inicializar phone y address con valores vacíos
                String phone = "";
                String address = "";

                Log.d("GoogleSignIn", "Usuario autenticado con Google: " + email);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db.collection("users").document(userId);

                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // **Usuario NO registrado en Firestore, guardarlo**
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", userId);
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("image", image);
                        userData.put("authMethod", "google");
                        userData.put("phone", phone);
                        userData.put("address", address);


                        userRef.set(userData)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado en Firestore con Google"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario en Firestore con Google", e));
                    } else {
                        Log.d("Firestore", "Usuario ya registrado en Firestore, actualizando actividad...");
                    }


                    // Guardar datos en SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("userId", userId);
                    editor.putString("name", name);
                    editor.putString("email", email);
                    editor.putString("image", image);
                    editor.putString("phone", phone);
                    editor.putString("userAddress", address);
                    editor.putString("authMethod", "google");
                    editor.apply();

                    // **Guardar en SQLite**
                    FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                    favoritesManager.addOrUpdateUser(userId, name, email, null, null, address, phone, image);

                    // **Sincronizar favoritos**
                    favoritesManager.syncFavorites(userId);

                    // **Escuchar cambios en tiempo real**
                    favoritesManager.listenForFavoriteChanges(userId);

                    // Redirigir a MainActivity
                    goToMainActivityWithEmail(userId, name, email);
                });

            }
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "Error en inicio de sesión: " + e.getStatusCode());
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
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
