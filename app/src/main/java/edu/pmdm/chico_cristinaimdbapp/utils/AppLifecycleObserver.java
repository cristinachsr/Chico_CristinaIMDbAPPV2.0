package edu.pmdm.chico_cristinaimdbapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import edu.pmdm.chico_cristinaimdbapp.database.FavoritesManager;
import edu.pmdm.chico_cristinaimdbapp.sync.FirestoreHelper;

public class AppLifecycleObserver implements LifecycleObserver {
    private final Context context;
    private final FavoritesManager favoritesManager;

    //contructor
    public AppLifecycleObserver(Context context) {
        this.context = context;
        this.favoritesManager = FavoritesManager.getInstance(context);
    }

    //metodo que se ejecuta cuando la aplicacion esta en primer plano
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        if (userId != null) {
            String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

            // Actualiza la base de datos local
            favoritesManager.addOrUpdateUser(userId, null, null, currentTime, null, null, null, null);

            // Guarda en Firestore
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.addActivityLog(userId, currentTime, null);
        }
    }

    //metodo que se ejecuta cuando la aplicacion pasa a segunco plano
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        if (userId != null) {
            String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

            // Actualiza la base de datos local
            favoritesManager.addOrUpdateUser(userId, null, null, null, currentTime, null, null, null);

            // Guarda en Firestore
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.addActivityLog(userId, null, currentTime);
        }
    }

}
