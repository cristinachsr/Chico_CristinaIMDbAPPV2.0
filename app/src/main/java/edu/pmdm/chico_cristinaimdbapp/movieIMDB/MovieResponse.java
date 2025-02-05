package edu.pmdm.chico_cristinaimdbapp.movieIMDB;

import java.util.List;

// Clase que representa la respuesta de la API cuando se solicitan las películas más populares
public class MovieResponse {
    private Data data; // Contiene toda la información de la respuesta

    public Data getData() {
        return data; // Devuelve el objeto Data con la información de las películas
    }

    // Clase interna que almacena la información de la respuesta
    public static class Data {
        private TopMeterTitles topMeterTitles; // Contiene la lista de películas populares

        public TopMeterTitles getTopMeterTitles() {
            return topMeterTitles; // Devuelve la lista de películas populares
        }
    }

    // Clase que representa la lista de películas populares
    public static class TopMeterTitles {
        private List<Edge> edges; // Lista de películas

        public List<Edge> getEdges() {
            return edges; // Devuelve la lista de películas
        }
    }


    public static class Edge {
        private Node node; // Nodo que representa una película individual

        public Node getNode() {
            return node; // Devuelve el nodo con los detalles de la película
        }
    }

    // Clase que almacena los detalles de una película individual
    public static class Node {
        private String id; // Identificador único de la película
        private TitleText titleText; // Título de la película
        private Plot plot; // Sinopsis o descripción de la película
        private ReleaseDate releaseDate; // Fecha de estreno de la película
        private RatingsSummary ratingsSummary; // Puntuación de la película
        private PrimaryImage primaryImage; // Imagen principal de la película

        public String getId() {
            return id; // Devuelve el ID de la película
        }

        public String getTitleText() {
            return titleText.getText(); // Devuelve el título de la película
        }

        public String getPlotText() {
            // Si hay sinopsis, la devuelve, si no, muestra un mensaje indicando que no hay información
            return plot != null ? plot.getPlainText() : "Descripción no disponible";
        }

        public String getReleaseDateString() {
            // Formatea la fecha de estreno como "Año-Mes-Día" o muestra un mensaje si no hay fecha
            return releaseDate != null ? releaseDate.getYear() + "-" + releaseDate.getMonth() + "-" + releaseDate.getDay() : "Fecha no disponible";
        }

        public double getRating() {
            // Devuelve la puntuación promedio de la película, si no hay datos, devuelve 0.0
            return ratingsSummary != null ? ratingsSummary.getAggregateRating() : 0.0;
        }

        public String getImageUrl() {
            // Devuelve la URL de la imagen de la película o una cadena vacía si no hay imagen disponible
            return primaryImage != null ? primaryImage.getUrl() : "";
        }
    }

    // Clase que almacena el título de la película
    public static class TitleText {
        private String text; // Texto del título

        public String getText() {
            return text; // Devuelve el título de la película
        }
    }

    // Clase que almacena la sinopsis de la película
    public static class Plot {
        private String plainText; // Descripción de la película en texto plano

        public String getPlainText() {
            return plainText; // Devuelve la sinopsis de la película
        }
    }

    // Clase que almacena la fecha de estreno de la película
    public static class ReleaseDate {
        private int year; // Año de estreno
        private int month; // Mes de estreno
        private int day; // Día de estreno

        public int getYear() {
            return year; // Devuelve el año de estreno
        }

        public int getMonth() {
            return month; // Devuelve el mes de estreno
        }

        public int getDay() {
            return day; // Devuelve el día de estreno
        }
    }

    // Clase que almacena la puntuación de la película
    public static class RatingsSummary {
        private double aggregateRating; // Puntuación promedio de la película

        public double getAggregateRating() {
            return aggregateRating; // Devuelve la puntuación de la película
        }
    }

    // Clase que almacena la imagen principal de la película
    public static class PrimaryImage {
        private String url; // URL de la imagen

        public String getUrl() {
            return url; // Devuelve la URL de la imagen
        }
    }
}
