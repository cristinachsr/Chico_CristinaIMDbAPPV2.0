package edu.pmdm.chico_cristinaimdbapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.api.TMDBApiService;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.MovieAdapterTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.MovieResponseTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.MovieTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.RetrofitClientTMDB;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Actividad que muestra una lista de películas en un RecyclerView
public class MovieListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMovies; // RecyclerView donde se mostrarán las películas
    private MovieAdapterTMDB movieAdapterTMDB; // Adaptador para manejar la lista de películas
    private List<MovieTMDB> movieList = new ArrayList<>(); // Lista de películas obtenidas de la API
    private TMDBApiService tmdbApiService; // Servicio para interactuar con la API de TMDB

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        recyclerViewMovies = findViewById(R.id.recycler_view);

        // Configura el RecyclerView con GridLayoutManager para mostrar 2 columnas
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewMovies.setLayoutManager(gridLayoutManager);

        // Crea el adaptador con la lista de películas vacía y asignarlo al RecyclerView
        movieAdapterTMDB = new MovieAdapterTMDB(this, movieList);
        recyclerViewMovies.setAdapter(movieAdapterTMDB);

        // Inicializa el servicio de la API de TMDB usando Retrofit
        tmdbApiService = RetrofitClientTMDB.getTMDBClient().create(TMDBApiService.class);

        // Obtener parámetros de búsqueda (género y año) desde la intención
        String genreId = getIntent().getStringExtra("genreId");
        String year = getIntent().getStringExtra("year");

        // Cargar las películas usando los parámetros recibidos
        loadMovies(genreId, year);
    }

    // Método para obtener las películas de la API de TMDB
    private void loadMovies(String genreId, String year) {
        tmdbApiService.discoverMovies("es-ES", false, 1, genreId, year)
                .enqueue(new Callback<MovieResponseTMDB>() {
                    @Override
                    public void onResponse(Call<MovieResponseTMDB> call, Response<MovieResponseTMDB> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Limpiar la lista anterior y agregar las nuevas películas
                            movieList.clear();
                            movieList.addAll(response.body().getMovies());
                            movieAdapterTMDB.notifyDataSetChanged(); // Notificar cambios al adaptador
                        } else {
                            Log.e("TMDB_ERROR", "Error en la respuesta: " + response.code());
                            Toast.makeText(MovieListActivity.this, "No se pudieron cargar las películas.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponseTMDB> call, Throwable t) {
                        Log.e("TMDB_FAILURE", "Error al conectar con la API", t);
                        Toast.makeText(MovieListActivity.this, "Error de conexión.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
