package edu.pmdm.chico_cristinaimdbapp.movieIMDB;

public class Movie {
    private String id; // Identificador único de la película
    private String title; // Título de la película
    private String imageUrl; // URL de la imagen de la película
    private String plot; // Descripción o sinopsis de la película
    private String releaseDate; // Fecha de estreno de la película
    private double rating; // Puntuación de la película

    // Constructor con todos los atributos
    public Movie(String id, String title, String imageUrl, String plot, String releaseDate, double rating) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.plot = plot;
        this.releaseDate = releaseDate;
        this.rating = rating;
    }

    // Constructor vacío (para cuando se necesite crear una película sin datos iniciales)
    public Movie() {
    }

    // Métodos getter para obtener los valores de los atributos
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPlot() {
        return plot;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public double getRating() {
        return rating;
    }

    // Métodos setter para modificar los valores de los atributos
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    @Override
    public boolean equals(Object obj) {
        // Comparamos las películas basándonos en su ID
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movie movie = (Movie) obj;
        return id.equals(movie.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode(); // Genera un hash basado en el ID de la película
    }
}
