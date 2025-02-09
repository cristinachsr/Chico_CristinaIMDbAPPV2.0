package edu.pmdm.chico_cristinaimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.KeystoreManager;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;

public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorites.db"; // Nombre de la base de datos
    private static final int DATABASE_VERSION = 3; // Versión de la base de datos para actualizaciones
    private final Context context;
    private final KeystoreManager keystoreManager;

    // Nombre de la tabla y columnas
    private static final String TABLE_FAVORITES = "favorites";
    private static final String COLUMN_ID = "id"; // ID de la película
    private static final String COLUMN_TITLE = "title"; // Título de la película
    private static final String COLUMN_IMAGE_URL = "imageUrl"; // Imagen de la película
    private static final String COLUMN_RELEASE_DATE = "releaseDate"; // Fecha de lanzamiento
    private static final String COLUMN_PLOT = "plot"; // Sinopsis
    private static final String COLUMN_RATING = "rating"; // Puntuación
    private static final String COLUMN_USER_ID = "user_id"; // Usuario que ha guardado la película

    // Tabla de usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERS_USER_ID = "user_id"; // Renombrada para evitar conflicto
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LOGIN_TIME = "login_time";
    public static final String COLUMN_LOGOUT_TIME = "logout_time";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";





    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.keystoreManager = new KeystoreManager(context);
        // 🔹 Esto asegurará que la base de datos se cree y se mantenga abierta
        getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear la tabla de favoritos con clave compuesta (ID de película y usuario)
        String createTable = "CREATE TABLE " + TABLE_FAVORITES + " (" +
                COLUMN_ID + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_IMAGE_URL + " TEXT, " +
                COLUMN_RELEASE_DATE + " TEXT, " +
                COLUMN_PLOT + " TEXT, " +
                COLUMN_RATING + " REAL, " +
                COLUMN_USER_ID + " TEXT, " +
                "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_USER_ID + "))"; // Clave compuesta para evitar duplicados por usuario
        db.execSQL(createTable);

        // Crear tabla de usuarios
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USERS_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT, " +
                COLUMN_LOGIN_TIME + " TEXT, " +
                COLUMN_LOGOUT_TIME + " TEXT, " +
                COLUMN_ADDRESS + " TEXT, " +
                COLUMN_PHONE + " TEXT, " +
                COLUMN_IMAGE + " TEXT)";
        db.execSQL(createUsersTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Crear tabla de usuarios si no existe
            String createUsersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COLUMN_USERS_USER_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_LOGIN_TIME + " TEXT, " +
                    COLUMN_LOGOUT_TIME + " TEXT, " +
                    COLUMN_ADDRESS + " TEXT, " +
                    COLUMN_PHONE + " TEXT, " +
                    COLUMN_IMAGE + " TEXT)";
            db.execSQL(createUsersTable);

            // Agregar relación entre favoritos y usuarios
            String addForeignKey = "PRAGMA foreign_keys = ON;";
            db.execSQL(addForeignKey);
        }
    }

    public void addFavorite(Movie movie, String userId) {
        // Verificar si la película ya está en favoritos para evitar duplicados
        if (movieExists(movie.getId(), userId)) {
            Log.d("FavoritesDatabaseHelper", "La película ya está en favoritos: " + movie.getId());
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, movie.getId());
        values.put(COLUMN_TITLE, movie.getTitle());
        values.put(COLUMN_IMAGE_URL, movie.getImageUrl());
        values.put(COLUMN_RELEASE_DATE, movie.getReleaseDate());
        values.put(COLUMN_PLOT, movie.getPlot());
        values.put(COLUMN_RATING, movie.getRating());
        values.put(COLUMN_USER_ID, userId); // Guardar el ID del usuario

        // Insertar la película en la base de datos
        long result = db.insert(TABLE_FAVORITES, null, values);
        if (result == -1) {
            Log.e("FavoritesDatabaseHelper", "Error al insertar la película en la base de datos: " + movie.getId());
        } else {
            Log.d("PeliculaFavoritos", "Película agregada con éxito: " + movie.getId());
        }
        //db.close();
    }

    private boolean movieExists(String movieId, String userId) {
        // Comprobar si una película ya está en la base de datos para ese usuario
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {movieId, userId};

        Cursor cursor = db.query(TABLE_FAVORITES, null, selection, selectionArgs, null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }

    public void removeFavorite(String movieId, String userId) {
        // Eliminar una película de la lista de favoritos
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?";
        String[] whereArgs = {movieId, userId};
        db.delete(TABLE_FAVORITES, whereClause, whereArgs);
        db.close();
    }

    public List<Movie> getAllFavorites(String userId) {
        // Obtener la lista de películas favoritas de un usuario
        List<Movie> favoriteMovies = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {userId};

        Cursor cursor = db.query(TABLE_FAVORITES, null, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Movie movie = new Movie();
                movie.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)));
                movie.setReleaseDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELEASE_DATE)));
                movie.setPlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLOT)));
                movie.setRating(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RATING)));
                favoriteMovies.add(movie);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return favoriteMovies;
    }

    public void clearFavorites(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_USER_ID + " = ?";
        String[] whereArgs = {userId};
        db.delete(TABLE_FAVORITES, whereClause, whereArgs);
        Log.d("FavoritesDatabaseHelper", "Favoritos limpiados para el usuario: " + userId);
        db.close();
    }


    /// Método para agregar usuario con cifrado de dirección y teléfono
    public void addUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Cifrar dirección y teléfono
            String encryptedAddress = address != null ? keystoreManager.encrypt(address) : null;
            String encryptedPhone = phone != null ? keystoreManager.encrypt(phone) : null;

            values.put(COLUMN_USERS_USER_ID, userId);
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_LOGIN_TIME, loginTime);
            values.put(COLUMN_LOGOUT_TIME, logoutTime);
            values.put(COLUMN_ADDRESS, encryptedAddress); // 🔒 Dirección cifrada
            values.put(COLUMN_PHONE, encryptedPhone); // 🔒 Teléfono cifrado
            values.put(COLUMN_IMAGE, image);

            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e("FavoritesDBHelper", "Error al cifrar los datos en addUser: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    // Método para actualizar usuario con cifrado de dirección y teléfono
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            if (name != null) values.put(COLUMN_NAME, name);
            if (email != null) values.put(COLUMN_EMAIL, email);
            if (loginTime != null) values.put(COLUMN_LOGIN_TIME, loginTime);
            if (logoutTime != null) values.put(COLUMN_LOGOUT_TIME, logoutTime);
            if (address != null) values.put(COLUMN_ADDRESS, keystoreManager.encrypt(address)); // 🔒 Cifrar dirección
            if (phone != null) values.put(COLUMN_PHONE, keystoreManager.encrypt(phone)); // 🔒 Cifrar teléfono
            if (image != null) values.put(COLUMN_IMAGE, image);

            db.update(TABLE_USERS, values, COLUMN_USERS_USER_ID + " = ?", new String[]{userId});
        } catch (Exception e) {
            Log.e("FavoritesDBHelper", "Error al cifrar los datos en updateUser: " + e.getMessage());
        } finally {
            db.close();
        }
    }


    public Cursor getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USERS_USER_ID + " = ?", new String[]{userId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String encryptedAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS));
                String encryptedPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE));

                // Descifrar dirección y teléfono
                String decryptedAddress = keystoreManager.decrypt(encryptedAddress);
                String decryptedPhone = keystoreManager.decrypt(encryptedPhone);

                Log.d("FavoritesDBHelper", "Dirección descifrada: " + decryptedAddress);
                Log.d("FavoritesDBHelper", "Teléfono descifrado: " + decryptedPhone);
            } catch (Exception e) {
                Log.e("FavoritesDBHelper", "Error al descifrar los datos: " + e.getMessage());
            }
        }

        return cursor;
    }


    public void deleteUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, COLUMN_USERS_USER_ID + " = ?", new String[]{userId}); // Cambiado a COLUMN_USERS_USER_ID
        db.close();
    }





}
