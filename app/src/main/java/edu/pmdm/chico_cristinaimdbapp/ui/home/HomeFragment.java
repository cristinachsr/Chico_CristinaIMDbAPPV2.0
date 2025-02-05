package edu.pmdm.chico_cristinaimdbapp.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.R;
import edu.pmdm.chico_cristinaimdbapp.api.IMDBApiService;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieAdapter;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieDetailsResponse;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieResponse;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Fragmento que muestra una lista de películas populares y permite añadirlas a favoritos
public class HomeFragment extends Fragment {

    private RecyclerView recyclerView; // Contenedor para mostrar la lista de películas
    private MovieAdapter movieAdapter; // Adaptador para manejar la presentación de las películas

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar la interfaz de usuario del fragmento desde el archivo XML
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Configurar el RecyclerView con un diseño de cuadrícula de 2 columnas
        recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Inicializar el adaptador con una lista vacía y asignarlo al RecyclerView
        movieAdapter = new MovieAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(movieAdapter);

        // Configurar el evento de clic largo para agregar películas a favoritos
        movieAdapter.setOnMovieLongClickListener(movie -> {
            // Obtener el ID del usuario desde las preferencias compartidas
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = sharedPreferences.getString("userId", null);

            if (userId == null) {
                // Si el usuario no está autenticado, mostrar un mensaje y salir
                Toast.makeText(getContext(), "Usuario no autenticado. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamar a la API para obtener información detallada de la película seleccionada
            IMDBApiService apiService = RetrofitClient.getClient().create(IMDBApiService.class);
            Call<MovieDetailsResponse> call = apiService.getMovieDetails(movie.getId());

            // Manejar la respuesta de la API
            call.enqueue(new Callback<MovieDetailsResponse>() {
                @Override
                public void onResponse(Call<MovieDetailsResponse> call, Response<MovieDetailsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        MovieDetailsResponse details = response.body();

                        // Actualizar la información de la película con los datos recibidos
                        movie.setPlot(details.getData().getTitle().getPlotText());
                        movie.setRating(details.getData().getTitle().getRating());

                        Log.d("MovieDetails", "Película agregada a favoritos: " + movie.getTitle());

                        // Intentar agregar la película a favoritos
                        if (FavoritesManager.getInstance(getContext()).addFavorite(movie, userId)) {
                            Toast.makeText(getContext(), movie.getTitle() + " ya está en favoritos", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), movie.getTitle() + " agregado a favoritos", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No se pudieron obtener detalles de la película", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MovieDetailsResponse> call, Throwable t) {
                    // Manejar errores de conexión con la API
                    Toast.makeText(getContext(), "Error al obtener detalles de la película", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Cargar las películas más populares desde la API
        loadTopMovies();

        return root; // Devolver la vista inflada del fragmento
    }

    // Método para obtener las películas más populares desde la API de IMDb
    private void loadTopMovies() {
        IMDBApiService apiService = RetrofitClient.getClient().create(IMDBApiService.class);
        Call<MovieResponse> call = apiService.getTopMovies("ALL");

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = new ArrayList<>();
                    List<MovieResponse.Edge> edges = response.body().getData().getTopMeterTitles().getEdges();

                    // Limitar la lista a las 10 primeras películas
                    int limit = Math.min(edges.size(), 10);
                    for (int i = 0; i < limit; i++) {
                        MovieResponse.Node node = edges.get(i).getNode();
                        if (node != null) {
                            movies.add(new Movie(
                                    node.getId(),
                                    node.getTitleText(),
                                    node.getImageUrl(),
                                    node.getPlotText(),
                                    node.getReleaseDateString(),
                                    node.getRating()
                            ));
                        }
                    }

                    // Actualizar la lista de películas en el adaptador
                    movieAdapter.updateMovies(movies);
                } else {
                    Log.e("API_ERROR", "Error en la respuesta: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                Log.e("API_ERROR", "Error al obtener la lista de películas", t);
            }
        });
    }
}
