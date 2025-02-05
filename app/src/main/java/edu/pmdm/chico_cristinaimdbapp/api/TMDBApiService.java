package edu.pmdm.chico_cristinaimdbapp.api;


import edu.pmdm.chico_cristinaimdbapp.movieTMDB.MovieResponseTMDB;
import edu.pmdm.chico_cristinaimdbapp.movieTMDB.TMDBGenreResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Headers;

public interface TMDBApiService {

    // Cabeceras necesarias para autenticar con la API de TMDB
    @Headers({
            "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NzBiNDFhY2Y1MWNlMzFkZmY4NDM0MjViYzZhZGUwYSIsIm5iZiI6MTczNjQxNzk5NC4yLCJzdWIiOiI2NzdmYTJjYTg5ZmM1ZDk0NDI0ZTkzNWYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.FQKjoTIqkZ2-KPK3mYDsooD3aULcWdryOKUDDB1oq34", // Token de acceso a la API
            "accept: application/json"
    })
    @GET("genre/movie/list") // Endpoint para obtener la lista de géneros
    Call<TMDBGenreResponse> getGenres(@Query("language") String language);
    // Este método nos devuelve una lista de géneros de películas.
    // Se le pasa el parámetro `language` para que los géneros aparezcan en el idioma deseado.

    @Headers({
            "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NzBiNDFhY2Y1MWNlMzFkZmY4NDM0MjViYzZhZGUwYSIsIm5iZiI6MTczNjQxNzk5NC4yLCJzdWIiOiI2NzdmYTJjYTg5ZmM1ZDk0NDI0ZTkzNWYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.FQKjoTIqkZ2-KPK3mYDsooD3aULcWdryOKUDDB1oq34",
            "accept: application/json"
    })
    @GET("discover/movie") // Endpoint para buscar películas con filtros específicos
    Call<MovieResponseTMDB> discoverMovies(
            @Query("language") String language,         // Idioma en el que queremos los resultados
            @Query("include_adult") boolean includeAdult, // Si queremos incluir películas para adultos (true/false)
            @Query("page") int page,                    // Página de resultados
            @Query("with_genres") String genreId,       // Filtro de género (ej: "28" para acción)
            @Query("primary_release_year") String year  // Año de estreno de la película
    );
    // Este método nos devuelve una lista de películas según los filtros que pasemos.
}