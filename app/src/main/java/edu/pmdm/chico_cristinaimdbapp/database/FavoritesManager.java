package edu.pmdm.chico_cristinaimdbapp.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;
import edu.pmdm.chico_cristinaimdbapp.sync.FirestoreHelper;

public class FavoritesManager {
    private static FavoritesManager instance; // Instancia única
    private FavoritesDatabaseHelper dbHelper; // Referencia a la base de datos

    private FavoritesManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);

        // Abrir la base de datos y habilitar claves foráneas
        SQLiteDatabase db = dbHelper.getWritableDatabase(); // 🔹 Inicializar la variable db
        db.execSQL("PRAGMA foreign_keys = ON;"); // 🔹 Habilitar claves foráneas
    }


    public static synchronized FavoritesManager getInstance(Context context) {
        // Patrón Singleton: si no existe la instancia, la creamos
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    public boolean addFavorite(Movie movie, String userId) {
        // Verificamos si la película tiene un ID válido antes de guardarla
        if (movie.getId() == null || movie.getId().isEmpty()) {
            Log.e("FavoritesManager", "El ID de la película es nulo o vacío. No se puede guardar.");
            return false;
        }

        // Comprobar si la película ya está en favoritos
        List<Movie> currentFavorites = dbHelper.getAllFavorites(userId);
        if (currentFavorites.contains(movie)) {
            Log.i("FavoritesManager", "La película ya está en favoritos: " + movie.getTitle());
            return true; // Ya estaba guardada
        } else {
            dbHelper.addFavorite(movie, userId);
            Log.i("FavoritesManager", "Película agregada a favoritos: " + movie.getTitle());

            // Guardar en Firestore
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.addFavorite(
                    userId,
                    movie.getId(),
                    movie.getTitle(),
                    movie.getImageUrl(),
                    movie.getReleaseDate(),
                    movie.getPlot(),
                    movie.getRating()
            );
            Log.i("FavoritesManager", "Película agregada a favoritos (Firestore): " + movie.getTitle());


            return false; // Se agregó correctamente
        }
    }

    public void removeFavorite(Movie movie, String userId) {
        // Eliminar una película de favoritos
        dbHelper.removeFavorite(movie.getId(), userId);

        // Eliminar de Firestore
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.removeFavorite(userId, movie.getId());
        Log.i("FavoritesManager", "Película eliminada de favoritos (Firestore): " + movie.getTitle());
    }

    public List<Movie> getFavoriteMovies(String userId) {
        // Obtener todas las películas favoritas del usuario
        return dbHelper.getAllFavorites(userId);
    }
    public void syncFavorites(String userId) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // Descargar favoritos desde Firestore y guardarlos en SQLite
        firestoreHelper.getFavorites(userId,
                favorites -> {
                    for (Map<String, Object> movieData : favorites) {
                        Movie movie = new Movie();
                        movie.setId((String) movieData.get("movieId"));
                        movie.setTitle((String) movieData.get("title"));
                        movie.setImageUrl((String) movieData.get("imageUrl"));
                        movie.setReleaseDate((String) movieData.get("releaseDate"));
                        movie.setPlot((String) movieData.get("plot"));
                        movie.setRating((Double) movieData.get("rating"));

                        // Guardar en SQLite
                        dbHelper.addFavorite(movie, userId);
                    }
                    Log.i("FavoritesManager", "📥 Sincronización completada (Firestore → SQLite)");

                    // 🔼 2️⃣ Subir los favoritos locales a Firestore si no están en la nube
                    List<Movie> localFavorites = dbHelper.getAllFavorites(userId);
                    for (Movie movie : localFavorites) {
                        boolean existsInCloud = favorites.stream().anyMatch(m -> m.get("movieId").equals(movie.getId()));

                        if (!existsInCloud) {
                            firestoreHelper.addFavorite(
                                    userId,
                                    movie.getId(),
                                    movie.getTitle(),
                                    movie.getImageUrl(),
                                    movie.getReleaseDate(),
                                    movie.getPlot(),
                                    movie.getRating()
                            );
                            Log.i("FavoritesManager", "📤 Subida a Firestore: " + movie.getTitle());
                        }
                    }
                },
                e -> Log.e("FavoritesManager", "Error al sincronizar favoritos: ", e)
        );
    }
    public void listenForFavoriteChanges(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).collection("favorites")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("FirestoreListener", "Error al escuchar cambios", e);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d("FirestoreListener", "Detectados cambios en favoritos. Sincronizando...");
                        // Limpiar y actualizar la base de datos local
                        dbHelper.clearFavorites(userId);
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Movie movie = new Movie();
                            movie.setId(doc.getId());
                            movie.setTitle(doc.getString("title"));
                            movie.setImageUrl(doc.getString("imageUrl"));
                            movie.setReleaseDate(doc.getString("releaseDate"));
                            movie.setPlot(doc.getString("plot"));
                            movie.setRating(doc.getDouble("rating"));
                            dbHelper.addFavorite(movie, userId);
                        }
                    }
                });
    }


    @SuppressLint("Range")
    public void addOrUpdateUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        Cursor cursor = dbHelper.getUser(userId);

        if (cursor != null && cursor.moveToFirst()) {
            dbHelper.updateUser(userId,
                    name != null ? name : cursor.getString(cursor.getColumnIndex("name")),
                    email != null ? email : cursor.getString(cursor.getColumnIndex("email")),
                    loginTime,
                    logoutTime,
                    address != null ? address : cursor.getString(cursor.getColumnIndex("address")),
                    phone != null ? phone : cursor.getString(cursor.getColumnIndex("phone")),
                    image != null ? image : cursor.getString(cursor.getColumnIndex("image")));

            Log.d("FavoritesManager", " Usuario actualizado en SQLite: " + userId);
        } else {
            dbHelper.addUser(userId,
                    name != null ? name : "Usuario",
                    email != null ? email : "Sin Email",
                    loginTime,
                    logoutTime,
                    address != null ? address : "Sin Dirección",
                    phone != null ? phone : "Sin Teléfono",
                    image != null ? image : null);

            Log.d("FavoritesManager", " Usuario insertado en SQLite: " + userId);
        }

        if (cursor != null) {
            cursor.close();
        }
    }



    @SuppressLint("Range")
    public Map<String, String> getUserDetails(String userId) {
        Cursor cursor = dbHelper.getUser(userId);
        Map<String, String> userData = new HashMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            userData.put("user_id", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_USERS_USER_ID)));
            userData.put("name", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_NAME)));
            userData.put("email", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_EMAIL)));
            userData.put("login_time", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME)));
            userData.put("logout_time", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME)));
            userData.put("address", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_ADDRESS)));
            userData.put("phone", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_PHONE)));
            userData.put("image", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_IMAGE)));

            cursor.close();
        } else {
            Log.w("FavoritesManager", "No se encontró información para el usuario: " + userId);
        }

        return userData;
    }

    public boolean isUserExists(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT user_id FROM users WHERE user_id = ?", new String[]{userId});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }




    // Obtener datos de un usuario
    @SuppressLint("Range")
    public Map<String, String> getUser(String userId) {
        Cursor cursor = dbHelper.getUser(userId);
        Map<String, String> userData = new HashMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            userData.put("user_id", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_USERS_USER_ID)));
            userData.put("name", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_NAME)));
            userData.put("email", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_EMAIL)));
            userData.put("login_time", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME)));
            userData.put("logout_time", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME)));
            userData.put("address", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_ADDRESS)));
            userData.put("phone", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_PHONE)));
            userData.put("image", cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_IMAGE)));

            cursor.close();
        }

        return userData;
    }

    // Eliminar un usuario
    public void deleteUser(String userId) {
        dbHelper.deleteUser(userId);
    }





}
