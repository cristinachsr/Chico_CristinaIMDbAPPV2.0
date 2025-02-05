package edu.pmdm.chico_cristinaimdbapp;

import android.Manifest;
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

import java.util.Locale;

public class DetailActivityTMDB extends AppCompatActivity {

    private static final int REQUEST_CONTACT_PERMISSION = 101;
    private static final int REQUEST_SMS_PERMISSION = 102;
    private static final int PICK_CONTACT_REQUEST = 103;

    private ImageView imageViewPoster;
    private TextView textViewTitle, textViewDescription, textViewReleaseDate, textViewRating;
    private Button buttonShare;

    private String movieDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_tmdb);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        imageViewPoster = findViewById(R.id.imageViewPoster);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewReleaseDate = findViewById(R.id.textViewReleaseDate);
        textViewRating = findViewById(R.id.textViewRating);
        buttonShare = findViewById(R.id.buttonShare);

        // Obtener los datos pasados por el Intent
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String releaseDate = getIntent().getStringExtra("releaseDate");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");

        String formattedRating = String.format(Locale.FRANCE, "%.1f", rating);

        // Construir los detalles de la película
        movieDetails = "Título: " + title + "\n" +
                "Descripción: " + description + "\n" +
                "Fecha de lanzamiento: " + releaseDate + "\n" +
                "Puntuación: " + rating;

        // Mostrar los datos
        textViewTitle.setText(title);
        textViewDescription.setText(description);
        textViewReleaseDate.setText("Fecha de lanzamiento: " + releaseDate);
        textViewRating.setText("Puntuación: " + formattedRating);
        Glide.with(this).load(imageUrl).into(imageViewPoster);

        // Configurar el botón para compartir
        buttonShare.setOnClickListener(v -> checkContactPermission());
    }

    private void checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT_PERMISSION);
        } else {
            pickContact();
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        pickContactLauncher.launch(intent); // Nueva forma de iniciar la actividad
    }

    private final ActivityResultLauncher<Intent> pickContactLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri contactUri = result.getData().getData();
                            String contactNumber = getContactNumber(contactUri);

                            // Verificar si el usuario tiene permiso para enviar SMS
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
                            } else {
                                sendSMS(contactNumber, movieDetails);
                            }
                        }
                    });


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri contactUri = data.getData();
            String contactNumber = getContactNumber(contactUri);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
            } else {
                sendSMS(contactNumber, movieDetails);
            }
        }
    }

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

    private void sendSMS(String phoneNumber, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }

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
