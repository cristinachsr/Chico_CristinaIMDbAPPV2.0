package edu.pmdm.chico_cristinaimdbapp.sync;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirestoreHelper {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_FAVORITES = "favorites";
    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();

    }

    public void addFavorite(String userId, String movieId, String title, String imageUrl, String releaseDate, String plot, double rating) {
        CollectionReference favoritesRef = db.collection("users").document(userId).collection("favorites");

        Map<String, Object> movie = new HashMap<>();
        movie.put("movieId", movieId);
        movie.put("title", title);
        movie.put("imageUrl", imageUrl);
        movie.put("releaseDate", releaseDate);
        movie.put("plot", plot);
        movie.put("rating", rating);

        favoritesRef.document(movieId).set(movie)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Película añadida a favoritos: " + title);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error al añadir película a favoritos", e);
                });
    }

    public void getFavorites(String userId, Consumer<List<Map<String, Object>>> onSuccess, Consumer<Exception> onFailure) {
        if (userId == null || userId.isEmpty()) {
            Log.e("FirestoreHelper", "El userId proporcionado es nulo o está vacío");
            if (onFailure != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onFailure.accept(new IllegalArgumentException("El userId es nulo o vacío"));
                }
            }
            return;
        }

        CollectionReference favoritesRef = db.collection("users").document(userId).collection("favorites");

        favoritesRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> favorites = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        favorites.add(document.getData());
                    }
                    if (onSuccess != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onSuccess.accept(favorites);
                        } else {
                            // Compatibilidad para versiones anteriores
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                new Handler(Looper.getMainLooper()).post(() -> onSuccess.accept(favorites));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error al obtener los favoritos: " + e.getMessage(), e);
                    if (onFailure != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onFailure.accept(e);
                        } else {
                            // Compatibilidad para versiones anteriores
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                new Handler(Looper.getMainLooper()).post(() -> onFailure.accept(e));
                            }
                        }
                    }
                });
    }




    public void removeFavorite(String userId, String movieId) {
        CollectionReference favoritesRef = db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FAVORITES);

        favoritesRef.document(movieId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreHelper", "Película eliminada de favoritos: " + movieId);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error al eliminar película de favoritos", e);
                });
    }


    public void addActivityLog(String userId, String loginTime, String logoutTime) {
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Obtener el array de activity_log
                List<Map<String, Object>> activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                if (activityLog == null) {
                    activityLog = new ArrayList<>();
                }

                if (!activityLog.isEmpty()) {
                    // Obtener el último registro
                    Map<String, Object> lastLog = activityLog.get(activityLog.size() - 1);

                    // Si el último registro tiene login_time pero no logout_time
                    if (lastLog.containsKey("login_time") && !lastLog.containsKey("logout_time") && logoutTime != null) {
                        // Completar el último registro con logout_time
                        lastLog.put("logout_time", logoutTime);
                    } else if (loginTime != null) {
                        // Crear un nuevo registro para un nuevo login
                        Map<String, Object> newLog = new HashMap<>();
                        newLog.put("login_time", loginTime);
                        activityLog.add(newLog);
                    }
                } else if (loginTime != null) {
                    // Si no hay registros previos, agregar el primer registro
                    Map<String, Object> newLog = new HashMap<>();
                    newLog.put("login_time", loginTime);
                    activityLog.add(newLog);
                }

                // Actualizar el array completo en Firestore
                userRef.update("activity_log", activityLog)
                        .addOnSuccessListener(aVoid -> Log.d("FirestoreHelper", "Historial de actividad actualizado correctamente."))
                        .addOnFailureListener(e -> Log.e("FirestoreHelper", "Error al actualizar el historial de actividad.", e));
            } else {
                Log.e("FirestoreHelper", "Usuario no encontrado en Firestore.");
            }
        }).addOnFailureListener(e -> Log.e("FirestoreHelper", "Error al obtener el usuario.", e));
    }

    public void fetchUserFromFirestore(String userId, Consumer<Map<String, String>> onSuccess, Consumer<Exception> onFailure) {
        if (userId == null || userId.isEmpty()) {
            Log.e("FirestoreHelper", " userId es nulo o vacío.");
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, String> userData = new HashMap<>();
                        userData.put("name", documentSnapshot.getString("name"));
                        userData.put("email", documentSnapshot.getString("email"));
                        userData.put("phone", documentSnapshot.getString("phone"));
                        userData.put("address", documentSnapshot.getString("address"));
                        userData.put("image", documentSnapshot.getString("image"));

                        if (onSuccess != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                onSuccess.accept(userData);
                            }
                        }

                        Log.d("FirestoreHelper", " Datos de usuario obtenidos desde Firestore: " + userData);
                    } else {
                        Log.e("FirestoreHelper", " Usuario no encontrado en Firestore.");
                        if (onFailure != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                onFailure.accept(new Exception("Usuario no encontrado en Firestore"));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", " Error al obtener datos de Firestore", e);
                    if (onFailure != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onFailure.accept(e);
                        }
                    }
                });
    }



}