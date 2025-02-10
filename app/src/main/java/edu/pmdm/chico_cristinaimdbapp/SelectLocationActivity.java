package edu.pmdm.chico_cristinaimdbapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SelectLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2;

    private TextView tvSelectedAddress;
    private Button btnConfirmAddress, btnSearchAddress;
    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddress;
    private PlacesClient placesClient;
    private FrameLayout mapContainer;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Inicializar Google Places
        Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        placesClient = Places.createClient(this);

        // Inicializar vistas
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnConfirmAddress = findViewById(R.id.btnConfirmAddress);
        btnSearchAddress = findViewById(R.id.btnSearchAddress);
        mapContainer = findViewById(R.id.mapContainer);

        // Ocultar el mapa al iniciar la actividad
        mapContainer.setVisibility(View.GONE);

        // Verificar permisos de ubicación
        checkLocationPermission();

        // Botón para abrir el buscador de direcciones de Google
        btnSearchAddress.setOnClickListener(v -> openPlaceSearch());

        btnConfirmAddress.setOnClickListener(v -> {
            if (selectedAddress != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedAddress", selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish(); // Finalizar la actividad para volver a EditUserActivity
            } else {
                Toast.makeText(this, "Por favor, seleccione una dirección", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //solicitar los permisos
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Manejar la respuesta de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Se necesita permiso de ubicación para usar el mapa", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //abre la interfaz de búsqueda de Google Places
    private void openPlaceSearch() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,        // ID del lugar
                Place.Field.LAT_LNG,   // Coordenadas
                Place.Field.ADDRESS    // Dirección completa
        );

        try {
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("SelectLocationActivity", "Error al abrir el buscador de direcciones", e);
            Toast.makeText(this, "No se pudo abrir el buscador de direcciones", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                // Obtener latitud, longitud y dirección
                selectedLatLng = place.getLatLng();
                selectedAddress = place.getAddress();
                tvSelectedAddress.setText(selectedAddress);

                // Mostrar el contenedor del mapa
                mapContainer.setVisibility(View.VISIBLE);

                // **Inicializar el mapa dinámicamente si no ha sido inicializado**
                if (mapFragment == null) {
                    mapFragment = SupportMapFragment.newInstance();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mapContainer, mapFragment)
                            .commit();

                    // Esperar a que el mapa esté listo antes de usarlo
                    mapFragment.getMapAsync(this);
                    Log.d("SelectLocationActivity", "Mapa inicializado después de seleccionar una dirección.");
                } else {
                    // Si el mapa ya está cargado, simplemente actualizar la ubicación
                    actualizarMapa();
                }
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("SelectLocationActivity", "Mapa inicializado correctamente.");

        // Si ya hay una ubicación seleccionada, mostrarla en el mapa
        if (selectedLatLng != null) {
            actualizarMapa();
        }
    }

    private void actualizarMapa() {
        if (mMap != null && selectedLatLng != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(selectedLatLng)
                    .title(selectedAddress));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            Log.d("SelectLocationActivity", "Mapa actualizado con nueva dirección.");
        } else {
            Log.e("SelectLocationActivity", "Error: El mapa no está listo para mostrar la ubicación.");
        }
    }

    // Método para obtener dirección desde coordenadas
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Dirección desconocida";
    }
}
