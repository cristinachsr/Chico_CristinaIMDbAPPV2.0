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

    private final PlacesClient placesClient;
    private List<AutocompletePrediction> predictionsList;

    public PlacesAutoCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        this.placesClient = placesClient;
        this.predictionsList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return predictionsList.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return predictionsList.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint != null && constraint.length() > 0) {
                    try {
                        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                .setQuery(constraint.toString())
                                .build();

                        placesClient.findAutocompletePredictions(request)
                                .addOnSuccessListener(response -> {
                                    predictionsList = response.getAutocompletePredictions();
                                    Log.d("AutoCompleteAdapter", "Predicciones encontradas: " + predictionsList.size());
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AutoCompleteAdapter", "Error al obtener predicciones", e);
                                    predictionsList = new ArrayList<>();
                                    notifyDataSetChanged();
                                });

                        results.values = predictionsList;
                        results.count = predictionsList.size();
                    } catch (Exception e) {
                        Log.e("AutoCompleteAdapter", "Error en performFiltering", e);
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.values != null) {
                    predictionsList = (List<AutocompletePrediction>) results.values;
                    notifyDataSetChanged();
                }
            }
        };
    }

    public CharSequence getItemText(int position) {
        return getItem(position).getFullText(null);
    }
}