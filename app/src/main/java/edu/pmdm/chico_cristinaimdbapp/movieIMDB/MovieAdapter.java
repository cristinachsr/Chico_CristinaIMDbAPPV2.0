package edu.pmdm.chico_cristinaimdbapp.movieIMDB;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.DetailActivity;
import edu.pmdm.chico_cristinaimdbapp.R;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private List<Movie> movieList; // Lista de películas que se mostrarán
    private Context context; // Contexto de la aplicación
    private OnMovieLongClickListener longClickListener; // Listener para detectar clics largos en las películas

    // Constructor que recibe el contexto y la lista de películas
    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList != null ? movieList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño XML de cada película en la lista
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Obtenemos la película en la posición actual
        Movie movie = movieList.get(position);

        // Establecemos el título en el TextView
        holder.titleTextView.setText(movie.getTitle());

        // Cargamos la imagen de la película usando Glide
        Glide.with(context)
                .load(movie.getImageUrl())
                .into(holder.movieImageView);

        // Cuando se hace clic en una película, abrimos la pantalla de detalles
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("movieId", movie.getId()); // Pasamos el ID de la película
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("imageUrl", movie.getImageUrl());
            intent.putExtra("description", movie.getPlot());
            intent.putExtra("releaseDate", movie.getReleaseDate());
            intent.putExtra("rating", movie.getRating());
            context.startActivity(intent);
        });

        // Cuando se hace un clic largo en una película, se ejecuta el listener
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMovieLongClick(movie);
            }
            return true; // Indica que el evento fue manejado
        });
    }

    public interface OnMovieLongClickListener {
        void onMovieLongClick(Movie movie); // Método que se ejecutará cuando se haga un clic largo en una película.
    }

    public void setOnMovieLongClickListener(OnMovieLongClickListener listener) {
        this.longClickListener = listener; // Guardamos la referencia del listener para usarlo en el adaptador.
    }

    @Override
    public int getItemCount() {
        return movieList.size(); // Devuelve la cantidad de películas en la lista
    }

    public void updateMovies(List<Movie> movies) {
        // Actualiza la lista de películas con nuevos datos
        this.movieList = movies != null ? movies : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Clase interna que representa cada elemento en la lista
    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView movieImageView;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.movie_title);
            movieImageView = itemView.findViewById(R.id.movie_image);
        }
    }
}
