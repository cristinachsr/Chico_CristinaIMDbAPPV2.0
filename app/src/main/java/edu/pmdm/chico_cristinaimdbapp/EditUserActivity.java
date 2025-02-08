package edu.pmdm.chico_cristinaimdbapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.sync.FirestoreHelper;


public class EditUserActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int SELECT_LOCATION_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private EditText editTextName, editTextPhone, editTextEmail;
    private ImageView imageViewProfile;
    private CountryCodePicker countryCodePicker;
    private String currentPhotoPath;
    private TextView textViewAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Vincular vistas
        editTextName = findViewById(R.id.editTextName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextEmail = findViewById(R.id.editTextEmail);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        countryCodePicker = findViewById(R.id.countryCodePicker);
        textViewAddress= findViewById(R.id.textViewAddress);

        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        Button buttonSave = findViewById(R.id.buttonSave);

        // Configurar CountryCodePicker con el campo de teléfono
        countryCodePicker.registerCarrierNumberEditText(editTextPhone);

        // Listener para seleccionar imagen
        buttonSelectImage.setOnClickListener(v -> showImagePickerDialog());

        Button buttonSelectAddress = findViewById(R.id.buttonSelectLocation);
        buttonSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EditUserActivity.this, SelectLocationActivity.class);
            startActivityForResult(intent, 100); // Cambiado a 100 para que coincida
        });

        // Listener para guardar datos
        buttonSave.setOnClickListener(v -> saveUserData());

        //recuperar datos de la base de datos
        loadUserData();
        checkAndRequestCameraPermission();

    }
    private void checkAndRequestCameraPermission() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), CAMERA_PERMISSION_REQUEST_CODE);
        }
    }


    private void showImagePickerDialog() {
        String[] options = {"Tomar foto", "Elegir de la galería"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
                }
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Si no hay cámara, mostrar un selector de aplicaciones
            Intent pickAppIntent = new Intent(Intent.ACTION_MAIN);
            pickAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            Intent chooserIntent = Intent.createChooser(pickAppIntent, "Selecciona una aplicación de cámara");
            startActivity(chooserIntent);
        }
    }





    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Verificar si hay una aplicación que maneje la acción
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } else {
            // Si no hay una galería, usar el selector de archivos
            Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fileIntent.setType("image/*");
            startActivityForResult(fileIntent, PICK_IMAGE_REQUEST);
        }
    }

    private void handleImageSelection(Uri imageUri) {
        if (imageUri != null) {
            // Si la imagen viene de la galería, conviértela a un archivo local
            String localPath = saveGalleryImageToLocalFile(imageUri);
            if (localPath != null) {
                imageViewProfile.setImageURI(Uri.fromFile(new File(localPath))); // Mostrar la imagen
                saveImageToPreferences(localPath); // Guardar la ruta del archivo local
            } else {
                Toast.makeText(this, "Error al guardar la imagen seleccionada", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveGalleryImageToLocalFile(Uri imageUri) {
        try {
            // Crear un archivo temporal en el almacenamiento interno
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(null);
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);

            // Abrir un flujo de entrada desde el URI de la galería
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // Abrir un flujo de salida hacia el archivo local
            OutputStream outputStream = new FileOutputStream(imageFile);

            // Copiar los datos de la galería al archivo local
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Cerrar los flujos
            inputStream.close();
            outputStream.close();

            // Retornar la ruta del archivo guardado
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la imagen seleccionada", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    private void saveImageToPreferences(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("profileImagePath", imagePath);
            editor.apply();
        } else {
            Toast.makeText(this, "Error: No se pudo guardar la imagen.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Manejo de imágenes (galería o cámara)
        if (resultCode == RESULT_OK) {
            Uri imageUri = null;

            // Imagen seleccionada desde la galería
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
            }
            // Imagen tomada desde la cámara
            else if (requestCode == CAPTURE_IMAGE_REQUEST) {
                File file = new File(currentPhotoPath);
                if (file.exists()) {
                    imageUri = Uri.fromFile(file);
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    if (bitmap != null) {
                        imageViewProfile.setImageBitmap(bitmap); // Mostrar la imagen inmediatamente
                        saveImageToPreferences(currentPhotoPath);
                    }
                }
            }

            if (imageUri != null) {
                handleImageSelection(imageUri);
            }


            // Manejo de la dirección seleccionada desde SelectLocationActivity
            if (requestCode == 100 && data != null) { //  Verificar que coincida con startActivityForResult()
                String selectedAddress = data.getStringExtra("selectedAddress");
                if (selectedAddress != null) {
                    TextView textViewAddress = findViewById(R.id.textViewAddress);
                    textViewAddress.setText(selectedAddress);

                    // Guardar dirección en SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("userAddress", selectedAddress);
                    editor.apply();

                    Log.d("direccion", "Dirección guardada en SharedPreferences: " + selectedAddress);
                }
            }
        }
    }

    private Uri saveBitmapToInternalStorage(Bitmap bitmap) {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            return null;
        }
    }


    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Log.e("EditUserActivity", " No se encontró userId en SharedPreferences.");
            Toast.makeText(this, "Error: No se encontró información del usuario.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cargar datos desde Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obtener datos desde Firestore
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");
                        String address = documentSnapshot.getString("address");
                        String imagePath = documentSnapshot.getString("image");

                        // Actualizar los campos en la interfaz de usuario
                        editTextName.setText(name != null ? name : "");
                        editTextEmail.setText(email != null ? email : "");
                        editTextEmail.setEnabled(false); // No permitir editar el correo
                        editTextPhone.setText(phone != null ? phone : "");
                        textViewAddress.setText(address != null ? address : "");

                        // Manejar la imagen del perfil
                        if (imagePath != null && !imagePath.isEmpty()) {
                            Glide.with(this).load(imagePath).circleCrop().into(imageViewProfile);
                        } else {
                            Glide.with(this).load("android.resource://" + getPackageName() + "/drawable/logoandroid").circleCrop().into(imageViewProfile);
                        }
                    } else {
                        Log.e("Firestore", " Usuario no encontrado en Firestore.");
                        Toast.makeText(this, "No se encontraron datos del usuario en la nube.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", " Error al obtener datos del usuario desde Firestore", e);
                    Toast.makeText(this, "Error al cargar datos del usuario.", Toast.LENGTH_SHORT).show();
                });
    }


    private void updateUI(Map<String, String> userData) {
        if (userData == null || userData.isEmpty()) {
            Log.e("EditUserActivity", " No hay datos para actualizar.");
            return;
        }

        editTextName.setText(userData.get("name") != null ? userData.get("name") : "");
        editTextEmail.setText(userData.get("email") != null ? userData.get("email") : "");
        editTextEmail.setEnabled(false); //  No permitir modificar el correo
        editTextPhone.setText(userData.get("phone") != null ? userData.get("phone") : "");
        textViewAddress.setText(userData.get("address") != null ? userData.get("address") : "");

        // Manejo seguro de la imagen
        String imagePath = userData.get("image");
        if (imagePath == null || imagePath.isEmpty()) {
            imagePath = "android.resource://" + getPackageName() + "/drawable/logoandroid";
        }

        Glide.with(this).load(imagePath).circleCrop().into(imageViewProfile);
    }

    private void saveUserData() {
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String address = textViewAddress.getText().toString().trim();

        if (!validateName(name)) return;
        if (!validatePhone()) return;

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        String email = sharedPreferences.getString("email", "Correo no disponible");

        // Verificar si ya hay una imagen guardada
        String imagePath = sharedPreferences.getString("profileImagePath", null);

        if (imagePath == null || imagePath.isEmpty()) {
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            Map<String, String> userData = favoritesManager.getUserDetails(userId);

            // Intenta obtener la imagen desde SQLite
            imagePath = userData != null ? userData.get("image") : null;

            // Si aún es nulo, usa la imagen predeterminada
            if (imagePath == null || imagePath.isEmpty()) {
                imagePath = "android.resource://" + getPackageName() + "/drawable/logoandroid";
            }
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error al guardar: Usuario no válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar en SQLite
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.addOrUpdateUser(userId, name, email, null, null, address, phone, imagePath);

        // Guardar en SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("phone", phone);
        editor.putString("userAddress", address);
        editor.putString("profileImagePath", imagePath); // Guardar correctamente la imagen actual
        editor.apply();

        // Guardar en Firestore
        saveUserToFirestore(userId, name, email, phone, address, imagePath);

        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(EditUserActivity.this, MainActivity.class));
        finish();
    }


    private void saveUserToFirestore(String userId, String name, String email, String phone, String address, String imagePath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name != null ? name : "Usuario");
        userData.put("email", email);
        userData.put("phone", phone != null ? phone : "");
        userData.put("address", address != null ? address : "");
        userData.put("image", imagePath != null ? imagePath : "android.resource://" + getPackageName() + "/drawable/logoandroid");

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", " Usuario actualizado en Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", " Error al actualizar usuario en Firestore", e));
    }

    private boolean validateName(String name) {
        if (name.isEmpty() || name.length() > 20 || !name.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
            Toast.makeText(this, "El nombre debe ser válido y contener solo letras", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validatePhone() {
        if (!countryCodePicker.isValidFullNumber()) {
            Toast.makeText(this, "Número de teléfono no válido según el país seleccionado", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }






}
