package edu.pmdm.chico_cristinaimdbapp.movieTMDB;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Clase que modela la respuesta de la API de TMDB cuando se solicitan películas
public class MovieResponseTMDB {

    // La API de TMDB devuelve la lista de películas en un campo llamado "results"
    @SerializedName("results")
    private List<MovieTMDB> movies;

    // Método para obtener la lista de películas de la respuesta
    public List<MovieTMDB> getMovies() {
        return movies;
    }

    // Método para establecer una nueva lista de películas en la respuesta
    public void setMovies(List<MovieTMDB> movies) {
        this.movies = movies;
    }
}
