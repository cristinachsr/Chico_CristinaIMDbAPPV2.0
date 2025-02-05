package edu.pmdm.chico_cristinaimdbapp.bluetooth;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.List;

import edu.pmdm.chico_cristinaimdbapp.movieIMDB.Movie;

public class BluetoothSimulator {

    private Context context; // Contexto de la aplicación, necesario para mostrar toasts y diálogos

    public BluetoothSimulator(Context context) {
        this.context = context; // Guardamos el contexto para poder usarlo en los métodos
    }

    public void simulateBluetoothConnection(List<Movie> favoriteMovies) {
        // Verificamos si la lista de películas favoritas está vacía o nula
        if (favoriteMovies == null || favoriteMovies.isEmpty()) {
            // Si no hay películas, mostramos un mensaje al usuario y salimos del método
            Toast.makeText(context, "No hay películas favoritas para compartir", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertimos la lista de películas favoritas a formato JSON usando Gson
        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(favoriteMovies);

        // Creamos un cuadro de diálogo para mostrar la lista de películas en formato JSON
        new AlertDialog.Builder(context)
                .setTitle("Películas Favoritas en JSON") // Título del cuadro de diálogo
                .setMessage(jsonFavorites) // Mostramos la lista de películas en formato JSON
                .setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cuando el usuario cierra el cuadro de diálogo, registramos en el log que finalizó la simulación
                        dialog.dismiss();
                        Log.d("BluetoothSimulator", "Simulación de Bluetooth finalizada.");
                        // También mostramos un mensaje al usuario indicando que las películas fueron "compartidas"
                        Toast.makeText(context, "Películas compartidas", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false) // Evita que el usuario cierre el diálogo tocando fuera de él
                .show(); // Muestra el cuadro de diálogo en pantalla
    }
}
