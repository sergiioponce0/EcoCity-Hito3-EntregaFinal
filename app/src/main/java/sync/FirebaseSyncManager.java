package sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.EcoCityDbHelper;
import model.Incidencia;
import com.example.ecocity.utils.NetworkUtils;

public class FirebaseSyncManager {
    
    private static final String TAG = "FirebaseSyncManager";
    private static final String COLECCION_INCIDENCIAS = "incidencias";
    
    private Context context;
    private FirebaseFirestore db;
    private EcoCityDbHelper dbHelper;
    private FirebaseAuth mAuth;
    
    public FirebaseSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.dbHelper = new EcoCityDbHelper(context);
        this.mAuth = FirebaseAuth.getInstance();
    }
    
    // ... (otros métodos como guardarIncidencia, sincronizarPendientes, etc. se mantienen igual)
    public void guardarIncidencia(Incidencia incidencia, final OnSyncListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No hay usuario autenticado");
            if (listener != null) {
                listener.onError("Usuario no autenticado");
            }
            return;
        }
        
        // Siempre guardar localmente primero
        long idLocal = dbHelper.insertarIncidencia(incidencia);
        
        if (NetworkUtils.isNetworkAvailable(context)) {
            // Hay red: sincronizar con Firestore
            Map<String, Object> incidenciaMap = convertirAMap(incidencia, user.getUid());
            
            db.collection(COLECCION_INCIDENCIAS)
                .add(incidenciaMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Incidencia sincronizada con ID: " + documentReference.getId());
                        // Marcar como sincronizada en la BD local
                        dbHelper.actualizarEstadoSync((int) idLocal, 1);
                        if (listener != null) {
                            listener.onSuccess(documentReference.getId());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al sincronizar", e);
                        if (listener != null) {
                            listener.onError(e.getMessage());
                        }
                    }
                });
        } else {
            // No hay red: queda pendiente de sincronización
            Log.d(TAG, "Sin conexión. Incidencia guardada localmente (pendiente de sync)");
            if (listener != null) {
                listener.onOffline(idLocal);
            }
        }
    }

    /**
     * ¡MÉTODO CORREGIDO! La estrategia ahora es "Local First".
     */
    public void obtenerIncidencias(final OnIncidenciasListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No hay usuario autenticado");
            if (listener != null) {
                listener.onError("Usuario no autenticado");
            }
            return;
        }

        // 1. Obtener y mostrar SIEMPRE los datos locales primero.
        List<Incidencia> locales = dbHelper.obtenerTodasIncidencias();
        if (listener != null) {
            // Notificamos inmediatamente con los datos locales para que la UI se actualice al instante.
            listener.onIncidenciasObtenidas(locales, false);
        }

        // 2. Si hay red, intentar obtener los datos de la nube en segundo plano.
        if (NetworkUtils.isNetworkAvailable(context)) {
            db.collection(COLECCION_INCIDENCIAS)
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incidencia> incidenciasDeFirestore = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Incidencia inc = convertirDesdeDocument(document);
                        incidenciasDeFirestore.add(inc);
                    }
                    
                    // Sincronizar la base de datos local con los datos de la nube
                    dbHelper.sincronizarDesdeFirestore(incidenciasDeFirestore);
                    
                    // Volver a cargar los datos locales ya sincronizados y notificar a la UI
                    List<Incidencia> actualizadas = dbHelper.obtenerTodasIncidencias();
                     if (listener != null) {
                        listener.onIncidenciasObtenidas(actualizadas, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener incidencias de Firestore. Los datos locales ya se mostraron.", e);
                    // No es necesario llamar a onError aquí, porque el usuario ya tiene los datos locales.
                });
        }
    }
    public void sincronizarPendientes(final OnSyncCompleteListener listener) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Sin conexión. No se pueden sincronizar pendientes.");
            if (listener != null) {
                listener.onComplete(0);
            }
            return;
        }
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No hay usuario autenticado");
            if (listener != null) {
                listener.onComplete(0);
            }
            return;
        }
        
        List<Incidencia> pendientes = dbHelper.obtenerIncidenciasPendientes();
        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay incidencias pendientes de sincronización");
            if (listener != null) {
                listener.onComplete(0);
            }
            return;
        }
        
        final int[] syncCount = {0};
        final int total = pendientes.size();
        
        for (final Incidencia incidencia : pendientes) {
            Map<String, Object> incidenciaMap = convertirAMap(incidencia, user.getUid());
            
            db.collection(COLECCION_INCIDENCIAS)
                .add(incidenciaMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Marcar como sincronizada
                        dbHelper.actualizarEstadoSync(incidencia.getId(), 1);
                        syncCount[0]++;
                        
                        if (syncCount[0] == total && listener != null) {
                            listener.onComplete(syncCount[0]);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al sincronizar incidencia pendiente", e);
                        syncCount[0]++;
                        
                        if (syncCount[0] == total && listener != null) {
                            listener.onComplete(syncCount[0]);
                        }
                    }
                });
        }
    }
    
    private Map<String, Object> convertirAMap(Incidencia incidencia, String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("titulo", incidencia.getTitulo());
        map.put("descripcion", incidencia.getDescripcion());
        map.put("urgencia", incidencia.getUrgencia());
        map.put("fecha", incidencia.getFecha());
        map.put("fotoUri", incidencia.getFotoUri());
        map.put("audioUri", incidencia.getAudioUri());
        map.put("latitud", incidencia.getLatitud());
        map.put("longitud", incidencia.getLongitud());
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }
    
    private Incidencia convertirDesdeDocument(QueryDocumentSnapshot document) {
        Incidencia inc = new Incidencia();
        inc.setId(-1); // ID local no aplica para datos de Firestore
        inc.setTitulo(document.getString("titulo"));
        inc.setDescripcion(document.getString("descripcion"));
        inc.setUrgencia(document.getString("urgencia"));
        inc.setFecha(document.getString("fecha"));
        inc.setFotoUri(document.getString("fotoUri"));
        inc.setAudioUri(document.getString("audioUri"));
        
        Double latitud = document.getDouble("latitud");
        Double longitud = document.getDouble("longitud");
        if (latitud != null) inc.setLatitud(latitud);
        if (longitud != null) inc.setLongitud(longitud);
        
        inc.setEstadoSync(1); // Marcada como sincronizada
        return inc;
    }
    
    // Interfaces de callback
    
    public interface OnSyncListener {
        void onSuccess(String firestoreId);
        void onError(String error);
        void onOffline(long localId);
    }
    
    public interface OnIncidenciasListener {
        void onIncidenciasObtenidas(List<Incidencia> incidencias, boolean fromCloud);
        void onError(String error);
    }
    
    public interface OnSyncCompleteListener {
        void onComplete(int count);
    }
}
