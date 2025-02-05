package edu.pmdm.chico_cristinaimdbapp.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.R;
import edu.pmdm.chico_cristinaimdbapp.api.TMDBApiService;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.MovieResponseTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.RetrofitClientTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.TMDBGenreResponse;
import edu.pmdm.chico_cristinaimdbapp.MovieListActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Fragmento que permite buscar películas en TMDB por género y año
public class SlideshowFragment extends Fragment {

    private Spinner spinnerGenres; // Selector de géneros de películas
    private EditText editTextYear; // Campo de entrada para el año de la película
    private Button buttonSearch; // Botón para iniciar la búsqueda

    private TMDBApiService tmdbApiService; // Servicio de la API de TMDB para obtener datos
    private List<TMDBGenreResponse.Genre> genreList = new ArrayList<>(); // Lista de géneros disponibles
    private ArrayAdapter<String> genreAdapter; // Adaptador para mostrar los géneros en el spinner

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el diseño del fragmento desde su XML
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        // Inicializar vistas
        spinnerGenres = root.findViewById(R.id.spinner_genres);
        editTextYear = root.findViewById(R.id.edittext_year);
        buttonSearch = root.findViewById(R.id.button_search);

        // Configurar la API Service para realizar consultas a TMDB
        tmdbApiService = RetrofitClientTMDB.getTMDBClient().create(TMDBApiService.class);

        // Llenar el Spinner con los géneros de películas
        loadGenres();

        // Deshabilitar el botón de búsqueda al inicio hasta que se ingrese un año válido
        buttonSearch.setEnabled(false);

        // Configurar validación del año ingresado por el usuario
        editTextYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Limitar el texto a 4 caracteres
                if (s.length() > 4) {
                    editTextYear.setText(s.subSequence(0, 4));
                    editTextYear.setSelection(4); // Mover el cursor al final
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Validar que el año sea un número de 4 dígitos dentro del rango permitido
                String year = s.toString();
                if (year.matches("\\d{4}") && Integer.parseInt(year) >= 1800 && Integer.parseInt(year) <= 2030) {
                    buttonSearch.setEnabled(true); // Habilitar botón si el año es válido
                } else {
                    buttonSearch.setEnabled(false); // Deshabilitar botón si el año es inválido
                    editTextYear.setError("Ingresa un año válido (1800-2030)");
                }
            }
        });

        // Configurar el botón de búsqueda
        buttonSearch.setOnClickListener(v -> {
            int selectedGenreIndex = spinnerGenres.getSelectedItemPosition(); // Obtener índice del género seleccionado
            String year = editTextYear.getText().toString(); // Obtener el año ingresado

            // Validar que se haya seleccionado un género
            if (selectedGenreIndex < 0 || selectedGenreIndex >= genreList.size()) {
                Toast.makeText(getContext(), "Por favor, selecciona un género", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el ID del género seleccionado
            String genreId = String.valueOf(genreList.get(selectedGenreIndex).getId());

            // Verificar si hay películas disponibles con los filtros ingresados
            checkMoviesAvailability(genreId, year);
        });

        return root; // Devolver la vista inflada
    }

    // Método para cargar la lista de géneros desde la API de TMDB
    private void loadGenres() {
        tmdbApiService.getGenres("es-ES").enqueue(new Callback<TMDBGenreResponse>() {
            @Override
            public void onResponse(Call<TMDBGenreResponse> call, Response<TMDBGenreResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Guardar la lista de géneros obtenida de la API
                    genreList = response.body().getGenres();

                    // Crear una lista con los nombres de los géneros
                    List<String> genreNames = new ArrayList<>();
                    for (TMDBGenreResponse.Genre genre : genreList) {
                        genreNames.add(genre.getName());
                    }

                    // Configurar el adaptador para mostrar los géneros en el Spinner
                    genreAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, genreNames);
                    genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerGenres.setAdapter(genreAdapter);
                } else {
                    Log.e("TMDB_ERROR", "Error al cargar géneros: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TMDBGenreResponse> call, Throwable t) {
                // Manejar errores de conexión con la API
                Log.e("TMDB_FAILURE", "Error al conectar con la API", t);
            }
        });
    }

    // Método para verificar si hay películas disponibles con los filtros seleccionados
    private void checkMoviesAvailability(String genreId, String year) {
        TMDBApiService tmdbApiService = RetrofitClientTMDB.getTMDBClient().create(TMDBApiService.class);

        tmdbApiService.discoverMovies("es-ES", false, 1, genreId, year)
                .enqueue(new Callback<MovieResponseTMDB>() {
                    @Override
                    public void onResponse(Call<MovieResponseTMDB> call, Response<MovieResponseTMDB> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().getMovies().isEmpty()) {
                            // Si hay películas disponibles, abrir la pantalla de resultados
                            Intent intent = new Intent(getContext(), MovieListActivity.class);
                            intent.putExtra("genreId", genreId);
                            intent.putExtra("year", year);
                            startActivity(intent);
                        } else {
                            // Si no hay películas disponibles, mostrar un mensaje
                            Toast.makeText(getContext(), "No hay películas disponibles para este año.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponseTMDB> call, Throwable t) {
                        // Manejar error de conexión con la API
                        Toast.makeText(getContext(), "Error al verificar películas. Por favor, intenta nuevamente.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
