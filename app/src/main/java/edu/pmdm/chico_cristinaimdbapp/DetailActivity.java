package edu.pmdm.chico_cristinaimdbapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import edu.pmdm.chico_cristinaimdbapp.api.IMDBApiService;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieDetailsResponse;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.Manifest;


// Actividad que muestra los detalles de una película y permite compartir la información por SMS.
public class DetailActivity extends AppCompatActivity {

    // Constantes para manejar permisos y solicitudes
    private static final int REQUEST_SMS_PERMISSION = 100; // Código para permisos de SMS
    private static final int REQUEST_CONTACT_PERMISSION = 101; // Código para permisos de contactos
    private static final int PICK_CONTACT_REQUEST = 102; // Código para la selección de contacto

    // Variable para almacenar los detalles de la película en formato de mensaje
    private String movieDetails = "";

    // Elementos de la interfaz de usuario
    private TextView titleTextView;
    private TextView plotTextView;
    private TextView releaseDateTextView;
    private TextView ratingTextView;
    private ImageView posterImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Obtener referencias a los componentes de la UI
        titleTextView = findViewById(R.id.detail_title);
        posterImageView = findViewById(R.id.detail_image);
        plotTextView = findViewById(R.id.detail_description);
        releaseDateTextView = findViewById(R.id.detail_release_date);
        ratingTextView = findViewById(R.id.detail_rating);

        // Obtener datos enviados desde la actividad anterior
        Intent intent = getIntent();
        String movieId = intent.getStringExtra("movieId");
        String title = intent.getStringExtra("title");
        String imageUrl = intent.getStringExtra("imageUrl");
        String releaseDate = intent.getStringExtra("releaseDate");

        // Establecer los datos en la interfaz
        titleTextView.setText(title);
        releaseDateTextView.setText("Fecha de lanzamiento: " + releaseDate);
        Glide.with(this).load(imageUrl).into(posterImageView); // Cargar imagen con Glide

        // Obtener detalles adicionales de la película desde la API
        fetchMovieDetails(movieId);

        // Construir un mensaje inicial con los detalles de la película
        movieDetails = "Esta película te gustará: " + title + "\n" +
                "Descripción: " + (plotTextView != null ? plotTextView : "No disponible") + "\n" +
                "Puntuación: " + ratingTextView;

        // Configurar botón para compartir por SMS
        Button sendSmsButton = findViewById(R.id.btn_send_sms);
        sendSmsButton.setOnClickListener(v -> checkContactPermission());
    }

    // Método para obtener los detalles de la película desde la API
    private void fetchMovieDetails(String movieId) {
        IMDBApiService apiService = RetrofitClient.getClient().create(IMDBApiService.class);

        apiService.getMovieDetails(movieId).enqueue(new Callback<MovieDetailsResponse>() {
            @Override
            public void onResponse(Call<MovieDetailsResponse> call, Response<MovieDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieDetailsResponse.Title movieTitle = response.body().getData().getTitle();

                    if (movieTitle != null) {
                        String plot = movieTitle.getPlotText();
                        double rating = movieTitle.getRating();

                        // Actualizar la UI con los detalles obtenidos
                        plotTextView.setText(plot);
                        ratingTextView.setText(String.format("Puntuación: %.1f", rating));

                        // Construir el mensaje de texto con los detalles de la película
                        movieDetails = "¡No te pierdas esta película!\n" +
                                "Título: " + movieTitle.getTitleText() + "\n" +
                                "Descripción: " + plot + "\n" +
                                "Fecha de lanzamiento: " + movieTitle.getReleaseDateString() + "\n" +
                                "Puntuación: " + String.format("%.1f", rating);
                    } else {
                        plotTextView.setText("Descripción no disponible");
                        ratingTextView.setText("Puntuación no disponible");
                    }
                } else {
                    plotTextView.setText("Error al cargar la descripción");
                    ratingTextView.setText("Error al cargar la puntuación");
                }
            }

            @Override
            public void onFailure(Call<MovieDetailsResponse> call, Throwable t) {
                plotTextView.setText("Error al conectar con la API");
                ratingTextView.setText("Error al conectar con la API");
            }
        });
    }

    // Método para verificar si el usuario tiene permiso para acceder a los contactos
    private void checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT_PERMISSION);
        } else {
            pickContact();
        }
    }

    // Método para abrir la lista de contactos y seleccionar uno
    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        pickContactLauncher.launch(intent); // Nuevo método recomendado
    }
    private final ActivityResultLauncher<Intent> pickContactLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri contactUri = result.getData().getData();
                            String contactNumber = getContactNumber(contactUri);

                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
                            }

                            else {
                                sendSMS(contactNumber, movieDetails);
                            }
                        }
                    });


    // Método que se ejecuta cuando el usuario selecciona un contacto
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri contactUri = data.getData();
            String contactNumber = getContactNumber(contactUri);

            // Verificar si se tiene permiso para enviar SMS
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
            } else {
                sendSMS(contactNumber, movieDetails);
            }
        }
    }

    // Método para obtener el número de teléfono del contacto seleccionado
    private String getContactNumber(Uri contactUri) {
        String number = "";
        try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                number = cursor.getString(columnIndex);
            }
        }
        return number;
    }

    // Método para enviar un SMS con los detalles de la película
    private void sendSMS(String phoneNumber, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }

    // Manejar los permisos solicitados por la aplicación
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CONTACT_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickContact();
        } else if (requestCode == REQUEST_SMS_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de SMS concedido. Selecciona un contacto nuevamente.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_SHORT).show();
        }
    }
}
