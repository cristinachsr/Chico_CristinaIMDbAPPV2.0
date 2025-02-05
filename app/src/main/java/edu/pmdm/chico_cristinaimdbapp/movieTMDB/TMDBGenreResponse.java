package edu.pmdm.chico_cristinaimdbapp.movieTMDB;

import java.util.List;

// Clase que representa la respuesta de la API de TMDB cuando se solicitan géneros de películas
public class TMDBGenreResponse {
    private List<Genre> genres; // Lista de géneros obtenidos de la API

    // Método para obtener la lista de géneros
    public List<Genre> getGenres() {
        return genres;
    }

    // Método para establecer una lista de géneros
    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    // Clase interna que representa un género individual
    public static class Genre {
        private int id; // ID del género
        private String name; // Nombre del género

        // Método para obtener el ID del género
        public int getId() {
            return id;
        }

        // Método para establecer el ID del género
        public void setId(int id) {
            this.id = id;
        }

        // Método para obtener el nombre del género
        public String getName() {
            return name;
        }

        // Método para establecer el nombre del género
        public void setName(String name) {
            this.name = name;
        }
    }
}
