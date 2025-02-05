package edu.pmdm.chico_cristinaimdbapp.movieTMDB;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Clase para gestionar la conexión con la API de TMDB usando Retrofit
public class RetrofitClientTMDB {

    // URL base de la API de TMDB
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3/";

    // Instancia única de Retrofit para evitar múltiples conexiones innecesarias
    private static Retrofit tmdbRetrofit;

    // Método para obtener una única instancia de Retrofit configurada para TMDB
    public static Retrofit getTMDBClient() {
        if (tmdbRetrofit == null) {
            // Configuración de Retrofit con la URL base y un convertidor Gson para procesar JSON
            tmdbRetrofit = new Retrofit.Builder()
                    .baseUrl(TMDB_BASE_URL) // Define la URL base para todas las solicitudes
                    .addConverterFactory(GsonConverterFactory.create()) // Convierte respuestas JSON en objetos Java
                    .build(); // Construye la instancia de Retrofit
        }
        return tmdbRetrofit;
    }
}
