package edu.pmdm.chico_cristinaimdbapp.movieTMDB;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.DetailActivityTMDB;
import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.R;
import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;

// Adaptador para el RecyclerView que muestra la lista de películas de TMDB
public class MovieAdapterTMDB extends RecyclerView.Adapter<MovieAdapterTMDB.MovieViewHolder> {

    private Context context; // Contexto de la aplicación
    private List<MovieTMDB> movieList; // Lista de películas a mostrar

    // Constructor que recibe el contexto y la lista de películas
    public MovieAdapterTMDB(Context context, List<MovieTMDB> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    // Infla el diseño de cada elemento de la lista
    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    // Enlaza los datos de la película con la vista
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        MovieTMDB movie = movieList.get(position);

        // Establece el título de la película
        holder.titleTextView.setText(movie.getTitle());

        // Construye la URL del póster de la película y carga la imagen con Glide
        String imageUrl = "https://image.tmdb.org/t/p/w500" + movie.getPosterPath();
        Glide.with(context).load(imageUrl).into(holder.posterImageView);

        // Configura el clic corto: Abre la actividad de detalles de la película
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivityTMDB.class);
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("description", movie.getOverview());
            intent.putExtra("releaseDate", movie.getReleaseDate());
            intent.putExtra("rating", movie.getRating());
            intent.putExtra("imageUrl", imageUrl);
            context.startActivity(intent);
        });

        // Configura el clic largo: Agrega la película a favoritos
        holder.itemView.setOnLongClickListener(v -> {
            SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = sharedPreferences.getString("userId", null);

            if (userId != null) {
                // Convierte la película TMDB en una película genérica Movie
                Movie favoriteMovie = movie.toMovie();

                // Agrega la película a favoritos
                boolean alreadyFavorite = FavoritesManager.getInstance(context).addFavorite(favoriteMovie, userId);

                if (alreadyFavorite) {
                    Toast.makeText(context, "La película ya está en favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, movie.getTitle() + " agregado a favoritos", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Usuario no autenticado. Inicia sesión.", Toast.LENGTH_SHORT).show();
            }

            return true; // Indica que el evento fue manejado
        });
    }

    // Devuelve la cantidad de elementos en la lista
    @Override
    public int getItemCount() {
        return movieList.size();
    }

    // Clase interna para manejar los elementos de la vista de cada película
    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImageView;
        TextView titleTextView;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.movie_image);
            titleTextView = itemView.findViewById(R.id.movie_title);
        }
    }
}
