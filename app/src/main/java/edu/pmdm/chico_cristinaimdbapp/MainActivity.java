package edu.pmdm.chico_cristinaimdbapp;

import static edu.pmdm.chico_cristinaimdbapp.IMDBApiClient.testApiKeyRotation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicReference;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.chico_cristinaimdbapp.sync.FirestoreHelper;
import edu.pmdm.chico_cristinaimdbapp.utils.AppLifecycleObserver;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        //metodo rotar api
        testApiKeyRotation();

        //Ciclo de vida
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver(this));
        Log.d("MainActivity", "Aplicación iniciada. Registrando login...");


        // Obtener datos del Intent (si vienen desde login)
        Intent intent = getIntent();
        String userIdFromIntent = intent.getStringExtra("userId");
        String nameFromIntent = intent.getStringExtra("name");
        String emailFromIntent = intent.getStringExtra("email");
        //String imageFromIntent = intent.getStringExtra("image");

        // Obtener datos desde SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", userIdFromIntent);
        String name = sharedPreferences.getString("name", nameFromIntent);
        String email = sharedPreferences.getString("email", emailFromIntent);
        String photoUrl = sharedPreferences.getString("photoUrl", null);
        String authMethod = sharedPreferences.getString("authMethod", null); // Obtener el método de autenticación
        String phone = sharedPreferences.getString("phone", "");  //  Si es null, dejarlo vacío
        String address = sharedPreferences.getString("userAddress", ""); //  Si es null, dejarlo vacío

        // Obtener la fecha y hora actuales
        String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());


        if (userId != null) {
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);

            // Actualizar el tiempo de login en la base de datos
            favoritesManager.addOrUpdateUser(userId, null, null, currentTime, null, null, null, null);

            Log.d("MainActivity", "Login registrado en la base de datos para el usuario: " + userId);
        } else {
            Log.w("MainActivity", "No se encontró un userId en SharedPreferences. No se registró el login.");
        }

        // Configuración de la vista principal con ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Configuración de la Toolbar
        setSupportActionBar(binding.appBarMain.toolbar);

        // Configuración del Navigation Drawer
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Configuración de la barra de navegación con las secciones del menú
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        // Configuración del controlador de navegación
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Acceder a la cabecera del menú de navegación
        View headerView = navigationView.getHeaderView(0);
        ImageView imageView = headerView.findViewById(R.id.imageView);
        TextView userName = headerView.findViewById(R.id.TextNombre);
        TextView userEmail = headerView.findViewById(R.id.TextCorreo);
        Button logoutButton = headerView.findViewById(R.id.button);


        //  Si hay userId en SharedPreferences, usarlo en vez de FirebaseAuth
        if (userId != null) {
            Log.d("MainActivity", "Usuario detectado en SharedPreferences: " + userId);

            //  Si el usuario se autenticó con Facebook, no usar FirebaseUser
            if ("facebook".equals(authMethod)) {
                Log.d("MainActivity", "Iniciando sesión con Facebook");
            } else if ("google".equals(authMethod)) {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                if (account != null) {
                    name = account.getDisplayName();
                    email = account.getEmail();
                    photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
                }
            }

            //  Sincronizar favoritos
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            favoritesManager.syncFavorites(userId);
            favoritesManager.listenForFavoriteChanges(userId);
            favoritesManager.addOrUpdateUser(userId, name, email, null, null, null, null, photoUrl);

            syncUserData(userId);

            //  Actualizar UI
            updateNavigationDrawer(name, email, photoUrl,authMethod);
        } else {
            Log.w("MainActivity", "No se encontró un usuario registrado. Redirigiendo a pantalla de login.");
            startActivity(new Intent(this, PantallaPrincipal.class));
            finish();
        }


        //  Actualizar el UI con los datos del usuario autenticado
        updateNavigationDrawer(name, email, photoUrl,authMethod);

        // Configurar el botón de cierre de sesión
        logoutButton.setOnClickListener(view -> signOut());


    }

    //sincronizar los datos de los usuarios
    private void syncUserData(String userId) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.fetchUserFromFirestore(userId, userData -> {
            // Validar campos obtenidos de Firestore
            String name = userData.containsKey("name") && userData.get("name") != null ? userData.get("name").toString() : "Usuario";
            String email = userData.containsKey("email") && userData.get("email") != null ? userData.get("email").toString() : "Correo no disponible";
            String address = userData.containsKey("address") && userData.get("address") != null ? userData.get("address").toString() : "";
            String phone = userData.containsKey("phone") && userData.get("phone") != null ? userData.get("phone").toString() : "";
            String image = userData.containsKey("image") && userData.get("image") != null ? userData.get("image").toString() : "android.resource://" + getPackageName() + "/drawable/logoandroid";

            // Guardar datos en SQLite
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            favoritesManager.addOrUpdateUser(userId, name, email, null, null, address, phone, image);

            // Guardar datos en SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("name", name);
            editor.putString("email", email);
            editor.putString("userAddress", address);
            editor.putString("phone", phone);
            editor.putString("profileImagePath", image);
            editor.apply();

            // Actualizar el menú lateral
            updateNavigationDrawer(name, email, image, sharedPreferences.getString("authMethod", null));

        }, e -> Log.e("MainActivity", "Error al obtener datos desde Firestore", e));
    }


    //actualizar el menu lateral
    private void updateNavigationDrawer(String name, String email, String photoUrl, String authMethod) {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        ImageView imageView = headerView.findViewById(R.id.imageView);
        TextView userName = headerView.findViewById(R.id.TextNombre);
        TextView userEmail = headerView.findViewById(R.id.TextCorreo);

        userName.setText(name);
        userEmail.setText(email);

        //  Verificar si el usuario se registró con Google y no tiene imagen
        if (photoUrl == null || photoUrl.isEmpty()) {
            if ("google".equals(authMethod)) {
                Log.d("NavigationDrawer", " Usuario de Google sin imagen, generando avatar.");
                photoUrl = "https://ui-avatars.com/api/?name=" + name + "&background=0D8ABC&color=fff";
            } else {
                Log.d("NavigationDrawer", " Usuario sin imagen, usando logoandroid.png.");
                photoUrl = "android.resource://" + getPackageName() + "/drawable/logoandroid";
            }
        }

        Log.d("NavigationDrawer", " Cargando imagen: " + photoUrl);

        Glide.with(this)
                .load(photoUrl)
                .fitCenter()
                .into(imageView);
    }
    //metodo cerrar sesion
    private void signOut() {
        Log.d("MainActivity", " Usuario cerrando sesión manualmente. Registrando logout...");

        // Obtener la fecha y hora actuales para registrar el logout
        String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

        // Obtener el userId desde SharedPreferences antes de borrar los datos
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        String authMethod = sharedPreferences.getString("authMethod", null); // Verificar método de autenticación

        if (userId != null) {
            // Registrar logout en la base de datos local
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            favoritesManager.addOrUpdateUser(userId, null, null, null, currentTime, null, null, null);
            Log.d("MainActivity", " Logout registrado en la base de datos local.");

            // Registrar logout en Firestore
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.addActivityLog(userId, null, currentTime); // Añadir evento al activity_log en Firestore
            Log.d("MainActivity", " Logout registrado en Firestore.");
        } else {
            Log.e("MainActivity", " No se pudo registrar logout porque el userId es nulo.");
        }

        // Cerrar sesión dependiendo del método de autenticación
        if ("google".equals(authMethod)) {
            // Cerrar sesión en Google
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> Log.d("MainActivity", " Sesión de Google cerrada."));
        } else if ("facebook".equals(authMethod)) {
            // Cerrar sesión en Facebook
            LoginManager.getInstance().logOut();
            Log.d("MainActivity", " Sesión de Facebook cerrada.");
        }

        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();
        Log.d("MainActivity", " Sesión de Firebase cerrada.");

        // Limpiar SharedPreferences para eliminar los datos del usuario
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Log.d("MainActivity", " SharedPreferences limpiadas, usuario completamente desconectado.");

        // Redirigir a la pantalla principal tras cerrar sesión
        Intent intent = new Intent(MainActivity.this, PantallaPrincipal.class);
        startActivity(intent);
        finish();
    }


    //maneja la selecion de opciones en el menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_edit_user) { // Detecta la opción "Editar Usuario"
            // Abre la actividad EditUserActivity
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //crea el menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //gestiona la navegacion
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    //metodo para el ciclo de vida
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Obtiene la hora actual en formato legible
        String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

        // Obtiene las preferencias compartidas para acceder al ID del usuario
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null); // Recupera el ID del usuario si está almacenado

        if (userId != null) { // Verifica si hay un usuario autenticado
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.addActivityLog(userId, null, currentTime); // Registra el cierre de sesión en Firestore

            // Guarda en `SharedPreferences` que el usuario ha cerrado sesión
            sharedPreferences.edit().putBoolean("wasLoggedOut", true).apply();

            Log.d("MainActivity", "✅ Logout registrado en Firestore al cerrar la app.");
        }
    }



}