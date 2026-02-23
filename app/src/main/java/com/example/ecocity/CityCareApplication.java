package com.example.ecocity;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Clase Application personalizada para CityCare.
 * Inicializa Firebase y configura Firestore para persistencia offline.
 */
public class CityCareApplication extends Application {
    
    private static final String TAG = "CityCareApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        
        // Configurar Firestore para persistencia offline
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        
        Log.d(TAG, "Firebase inicializado correctamente");
    }
}
