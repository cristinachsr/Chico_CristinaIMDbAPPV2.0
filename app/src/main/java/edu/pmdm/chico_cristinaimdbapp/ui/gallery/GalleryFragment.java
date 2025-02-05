package edu.pmdm.chico_cristinaimdbapp.ui.gallery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.PantallaPrincipal;
import edu.pmdm.chico_cristinaimdbapp.R;
import edu.pmdm.chico_cristinaimdbapp.bluetooth.BluetoothSimulator;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieAdapter;

// Fragmento que muestra la lista de películas favoritas y permite compartirlas por Bluetooth.
public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;  // Lista de películas favoritas
    private MovieAdapter movieAdapter;  // Adaptador para mostrar las películas en la lista
    private List<Movie> favoriteMovies = new ArrayList<>();  // Lista de películas favoritas

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el diseño del fragmento
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Obtener el userId del usuario desde SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            // Si el usuario no está autenticado, mostrar un mensaje y redirigir a la pantalla principal
            Toast.makeText(getContext(), "Usuario no autenticado. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), PantallaPrincipal.class);
            startActivity(intent);
            return root;  // No continuar con la ejecución del fragmento
        }

        // Configuración del RecyclerView para mostrar la lista de películas favoritas en 2 columnas
        recyclerView = root.findViewById(R.id.recycler_favorites);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Obtener la lista de películas favoritas del usuario desde la base de datos
        favoriteMovies = FavoritesManager.getInstance(getContext()).getFavoriteMovies(userId);
        Log.d("GalleryFragment", "Número de favoritos cargados: " + favoriteMovies.size());

        // Inicializar el adaptador con la lista de películas favoritas y asignarlo al RecyclerView
        movieAdapter = new MovieAdapter(getContext(), favoriteMovies);
        recyclerView.setAdapter(movieAdapter);
        movieAdapter.notifyDataSetChanged();  // Notificar al adaptador que los datos han cambiado

        // Configurar el listener para eliminar películas de favoritos al hacer un clic largo
        movieAdapter.setOnMovieLongClickListener(movie -> {
            // Eliminar la película de la lista de favoritos
            FavoritesManager.getInstance(getContext()).removeFavorite(movie, userId);
            // Actualizar la lista de películas en el adaptador después de eliminarla
            movieAdapter.updateMovies(FavoritesManager.getInstance(getContext()).getFavoriteMovies(userId));
            Log.d("GalleryFragment", "Película eliminada de favoritos: " + movie.getTitle());
            Toast.makeText(getContext(), movie.getTitle() + " eliminado de favoritos", Toast.LENGTH_SHORT).show();
        });

        // Configurar el botón para compartir las películas favoritas por Bluetooth
        Button btnShareBluetooth = root.findViewById(R.id.btn_share_bluetooth);
        btnShareBluetooth.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Solicitar permisos de Bluetooth si son necesarios
                requestBluetoothPermissions();
            } else {
                // Compartir las películas favoritas por Bluetooth
                shareFavoritesViaBluetooth();
            }
        });

        return root;  // Devolver la vista inflada
    }

    // Método para compartir las películas favoritas mediante Bluetooth
    private void shareFavoritesViaBluetooth() {
        // Obtener el adaptador de Bluetooth del dispositivo
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Si el dispositivo no tiene Bluetooth, mostrar un mensaje de error
            Toast.makeText(getContext(), "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Si Bluetooth no está habilitado, solicitar activación
            requestEnableBluetooth();
            return;
        }

        // Obtener el userId del usuario para recuperar sus películas favoritas
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            // Si no se encuentra el usuario, mostrar un mensaje de error
            Toast.makeText(getContext(), "No se encontró el usuario. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la lista de películas favoritas desde la base de datos
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(requireContext());
        List<Movie> favoriteMovies = dbHelper.getAllFavorites(userId);

        if (favoriteMovies.isEmpty()) {
            // Si el usuario no tiene películas favoritas, mostrar un mensaje de error
            Toast.makeText(getContext(), "No hay películas favoritas para compartir", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulación de conexión Bluetooth para compartir las películas
        BluetoothSimulator bluetoothSimulator = new BluetoothSimulator(getContext());
        bluetoothSimulator.simulateBluetoothConnection(favoriteMovies);
    }

    // Método para solicitar permisos de Bluetooth en Android 12+
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
            }, 100);
        }
    }

    // Método para solicitar la activación del Bluetooth
    private ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Si el usuario activó Bluetooth, intentar compartir las películas nuevamente
                    Toast.makeText(getContext(), "Bluetooth activado", Toast.LENGTH_SHORT).show();
                    shareFavoritesViaBluetooth();
                } else {
                    // Si el usuario canceló la activación, mostrar un mensaje de error
                    Toast.makeText(getContext(), "Bluetooth no activado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Método para solicitar la activación de Bluetooth si está desactivado
    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetoothLauncher.launch(enableBtIntent);
    }
}

