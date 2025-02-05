package edu.pmdm.chico_cristinaimdbapp;

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

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();

                            // Construir la solicitud con la clave API actual
                            Request request = original.newBuilder()
                                    .header("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                                    .header("x-rapidapi-key", RapidApiKeyManager.getApiKey())
                                    .method(original.method(), original.body())
                                    .build();

                            System.out.println("[IMDBApiClient] Enviando solicitud a API con clave: " + RapidApiKeyManager.getApiKey());

                            Response response = chain.proceed(request);

                            // Si hay un error 401 o 429, rota la clave API y reintenta
                            if (response.code() == 401 || response.code() == 429) {
                                System.out.println("[IMDBApiClient] Error " + response.code() + " detectado. Rotando clave API.");
                                response.close();
                                RapidApiKeyManager.rotateApiKey();

                                // Reintenta con la nueva clave
                                Request newRequest = original.newBuilder()
                                        .header("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                                        .header("x-rapidapi-key", RapidApiKeyManager.getApiKey())
                                        .method(original.method(), original.body())
                                        .build();

                                return chain.proceed(newRequest);
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

    public static IMDBApiService getService() {
        return getClient().create(IMDBApiService.class);
    }
}
