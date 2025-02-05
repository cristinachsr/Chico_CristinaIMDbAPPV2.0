package edu.pmdm.chico_cristinaimdbapp.movieIMDB;

import edu.pmdm.chico_cristinaimdbapp.IMDBApiClient;
import retrofit2.Retrofit;

// Clase que reutiliza la configuración de `IMDBApiClient`
public class RetrofitClient {
    // En lugar de crear una nueva instancia, reutilizamos `IMDBApiClient`
    public static Retrofit getClient() {
        return IMDBApiClient.getClient();
    }
}