package edu.pmdm.chico_cristinaimdbapp.api;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieDetailsResponse;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.MovieResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface IMDBApiService {

    // Cabeceras necesarias para autenticar con la API de IMDb

    @GET("title/get-top-meter") // Endpoint para obtener las películas más populares
    Call<MovieResponse> getTopMovies(@Query("topMeterTitlesType") String type);
    // Llamamos a este método para obtener el ranking de películas.
    // `type` define el tipo de ranking que queremos (por ejemplo, "ALL" para todas las categorías).


    @GET("title/get-overview") // Endpoint para obtener los detalles de una película
    Call<MovieDetailsResponse> getMovieDetails(@Query("tconst") String movieId);
    // Aquí pasamos el ID de la película (`tconst`), y la API nos devuelve información detallada sobre ella.
}

