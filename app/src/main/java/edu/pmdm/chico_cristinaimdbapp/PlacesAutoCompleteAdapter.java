package edu.pmdm.chico_cristinaimdbapp;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private final PlacesClient placesClient; // Cliente de Google Places para realizar solicitudes de autocompletado
    private List<AutocompletePrediction> predictionsList; // Lista de predicciones de autocompletado

    public PlacesAutoCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>()); // Se usa un diseño de lista simple para las sugerencias
        this.placesClient = placesClient; // Inicializa el cliente de Places
        this.predictionsList = new ArrayList<>(); // Inicializa la lista vacía
    }

    // Obtiene el número total de predicciones disponibles.
    @Override
    public int getCount() {
        return predictionsList.size();
    }

    //Obtiene una predicción específica en una posición dada.
    @Override
    public AutocompletePrediction getItem(int position) {
        return predictionsList.get(position);
    }

    //Devuelve el `Filter` utilizado para obtener predicciones de autocompletado.
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults(); // Objeto que contendrá los resultados del filtro

                if (constraint != null && constraint.length() > 0) { // Verifica si el usuario ha escrito algo
                    try {
                        // Crea una solicitud de autocompletado con la consulta del usuario
                        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                .setQuery(constraint.toString()) // Establece la consulta con el texto ingresado
                                .build();

                        // Envía la solicitud al cliente de Places
                        placesClient.findAutocompletePredictions(request)
                                .addOnSuccessListener(response -> {
                                    predictionsList = response.getAutocompletePredictions(); // Almacena las predicciones obtenidas
                                    Log.d("AutoCompleteAdapter", "Predicciones encontradas: " + predictionsList.size());
                                    notifyDataSetChanged(); // Notifica a la interfaz que los datos han cambiado
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AutoCompleteAdapter", "Error al obtener predicciones", e);
                                    predictionsList = new ArrayList<>(); // Limpia la lista en caso de error
                                    notifyDataSetChanged(); // Notifica a la interfaz
                                });

                        results.values = predictionsList; // Almacena las predicciones en los resultados
                        results.count = predictionsList.size(); // Asigna la cantidad de predicciones
                    } catch (Exception e) {
                        Log.e("AutoCompleteAdapter", "Error en performFiltering", e);
                    }
                }
                return results; // Devuelve los resultados filtrados
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.values != null) {
                    predictionsList = (List<AutocompletePrediction>) results.values; // Actualiza la lista de predicciones
                    notifyDataSetChanged(); // Refresca la interfaz para mostrar las nuevas sugerencias
                }
            }
        };
    }

}