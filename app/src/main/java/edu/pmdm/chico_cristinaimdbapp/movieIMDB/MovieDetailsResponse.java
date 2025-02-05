package edu.pmdm.chico_cristinaimdbapp.movieIMDB;

// Clase que representa la respuesta de la API cuando se solicitan detalles de una película
public class MovieDetailsResponse {
    private Data data; // Objeto que contiene los datos de la película

    public Data getData() {
        return data; // Método para obtener los datos de la película
    }

    // Clase interna que almacena la información principal de la película
    public static class Data {
        private Title title; // Objeto con los detalles del título de la película

        public Title getTitle() {
            return title; // Método para obtener los detalles del título
        }
    }

    // Clase que representa los detalles de una película
    public static class Title {
        private String id; // Identificador único de la película
        private TitleText titleText; // Título de la película
        private Plot plot; // Descripción de la película
        private ReleaseDate releaseDate; // Fecha de estreno
        private RatingsSummary ratingsSummary; // Puntuación de la película
        private PrimaryImage primaryImage; // Imagen principal de la película

        public String getId() {
            return id; // Devuelve el ID de la película
        }

        public String getTitleText() {
            // Devuelve el título de la película o "Sin título" si no está disponible
            return titleText != null ? titleText.getText() : "Sin título";
        }

        public String getPlotText() {
            // Devuelve la sinopsis de la película o un mensaje si no está disponible
            return plot != null && plot.getPlotText() != null ? plot.getPlotText().getPlainText() : "Descripción no disponible";
        }

        public String getReleaseDateString() {
            // Formatea la fecha de estreno en formato Año-Mes-Día, o muestra "Fecha no disponible" si falta algún dato
            return releaseDate != null ? releaseDate.getYear() + "-" + releaseDate.getMonth() + "-" + releaseDate.getDay() : "Fecha no disponible";
        }

        public double getRating() {
            // Devuelve la puntuación promedio de la película, o 0.0 si no hay datos disponibles
            return ratingsSummary != null ? ratingsSummary.getAggregateRating() : 0.0;
        }

        public String getImageUrl() {
            // Devuelve la URL de la imagen de la película o una cadena vacía si no está disponible
            return primaryImage != null ? primaryImage.getUrl() : "";
        }
    }

    // Clase que almacena el título de la película
    public static class TitleText {
        private String text; // Título de la película

        public String getText() {
            return text; // Devuelve el título de la película
        }
    }

    // Clase que representa la sinopsis de la película
    public static class Plot {
        private PlotText plotText; // Texto de la sinopsis

        public PlotText getPlotText() {
            return plotText; // Devuelve la sinopsis de la película
        }

        // Clase interna que almacena la sinopsis en formato de texto plano
        public static class PlotText {
            private String plainText; // Texto de la sinopsis

            public String getPlainText() {
                return plainText; // Devuelve la sinopsis en texto plano
            }
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
            return aggregateRating; // Devuelve la puntuación promedio
        }
    }

    // Clase que almacena la imagen principal de la película
    public static class PrimaryImage {
        private String url; // URL de la imagen de la película

        public String getUrl() {
            return url; // Devuelve la URL de la imagen
        }
    }
}
