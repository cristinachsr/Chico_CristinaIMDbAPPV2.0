package edu.pmdm.chico_cristinaimdbapp.movieTMDB;

import com.google.gson.annotations.SerializedName;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;

// Clase que modela una película obtenida desde la API de TMDB
public class MovieTMDB {
    private int id; // Identificador único de la película

    @SerializedName("title")
    private String title; // Título de la película

    @SerializedName("poster_path")
    private String posterPath; // Ruta del póster de la película

    @SerializedName("overview")
    private String overview; // Sinopsis de la película

    @SerializedName("release_date")
    private String releaseDate; // Fecha de estreno de la película

    @SerializedName("vote_average")
    private double rating; // Calificación promedio de la película

    // Constructor vacío
    public MovieTMDB() {}

    // Constructor con parámetros para inicializar los atributos de la película
    public MovieTMDB(int id, String title, String posterPath, String overview, String releaseDate, double rating) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
    }

    // Métodos getter y setter para acceder y modificar los atributos

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    // Método para obtener la URL completa de la imagen del póster
    public String getFullImageUrl() {
        return "https://image.tmdb.org/t/p/w500" + this.posterPath;
    }

    // Método que convierte un objeto MovieTMDB en un objeto Movie para la lista de favoritos y poder
    //llevarlo a la base de datos.
    public Movie toMovie() {
        Movie movie = new Movie();
        movie.setId(String.valueOf(this.id));
        movie.setTitle(this.title);
        movie.setImageUrl("https://image.tmdb.org/t/p/w500" + this.posterPath);
        movie.setReleaseDate(this.releaseDate);
        movie.setPlot(this.overview);
        movie.setRating(this.rating);
        return movie;
    }
}
