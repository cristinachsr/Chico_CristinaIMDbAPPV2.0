package edu.pmdm.chico_cristinaimdbapp;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import edu.pmdm.chico_cristinaimdbapp.api.IMDBApiService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IMDBApiClient {
    private static final String BASE_URL = "https://imdb-com.p.rapidapi.com/";

    private static Retrofit retrofit = null;

    //metodo para obtener la instancia de retrofit
    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String apiKey = RapidApiKeyManager.getApiKey(); // Obtener clave actual

                            // Log solo cuando se usa realmente la clave
                            Log.i(TAG, "Usando clave API para solicitud: " + apiKey);

                            // Primera solicitud con la clave actual
                            Request request = original.newBuilder()
                                    .header("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                                    .header("x-rapidapi-key", RapidApiKeyManager.getApiKey())
                                    .method(original.method(), original.body())
                                    .build();


                            Response response = chain.proceed(request);

                            // Verificar el código de respuesta
                            int responseCode = response.code();


                            // Si hay un error 401 o 429, rotar clave API y reintentar
                            if (responseCode == 401 || responseCode == 429) {
                                response.close();
                                RapidApiKeyManager.rotateApiKey();

                                // Nueva solicitud con la nueva clave API
                                Request newRequest = original.newBuilder()
                                        .header("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                                        .header("x-rapidapi-key", RapidApiKeyManager.getApiKey())
                                        .method(original.method(), original.body())
                                        .build();

                                return chain.proceed(newRequest);
                            }

                            //  Si hay otro error (500 o 403), también rotar clave
                            if (responseCode >= 500 || responseCode == 403) {
                                response.close();
                                RapidApiKeyManager.rotateApiKey();

                                Request retryRequest = original.newBuilder()
                                        .header("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                                        .header("x-rapidapi-key", RapidApiKeyManager.getApiKey())
                                        .method(original.method(), original.body())
                                        .build();

                                return chain.proceed(retryRequest);
                            }

                            return response;
                        }
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    //Método de prueba para verificar la rotación de claves API.
    public static void testApiKeyRotation() {
        for (int i = 0; i < 5; i++) { // Intentar cambiar de clave varias veces
            RapidApiKeyManager.rotateApiKey();
        }
    }

}
