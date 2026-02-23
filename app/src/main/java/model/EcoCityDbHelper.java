package model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class EcoCityDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2; // Subimos la versión para el nuevo campo
    private static final String DATABASE_NAME = "EcoCity.db";

    public static final String TABLE_INCIDENCIAS = "incidencias";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITULO = "titulo";
    public static final String COLUMN_DESCRIPCION = "descripcion";
    public static final String COLUMN_URGENCIA = "urgencia";
    public static final String COLUMN_FECHA = "fecha";
    public static final String COLUMN_FOTO_URI = "foto_uri";
    public static final String COLUMN_AUDIO_URI = "audio_uri";
    public static final String COLUMN_LATITUD = "latitud";
    public static final String COLUMN_LONGITUD = "longitud";
    public static final String COLUMN_ESTADO_SYNC = "estado_sync";
    public static final String COLUMN_CATEGORIA = "categoria"; // Nuevo campo

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_INCIDENCIAS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITULO + " TEXT," +
                    COLUMN_DESCRIPCION + " TEXT," +
                    COLUMN_URGENCIA + " TEXT," +
                    COLUMN_FECHA + " TEXT," +
                    COLUMN_FOTO_URI + " TEXT," +
                    COLUMN_AUDIO_URI + " TEXT," +
                    COLUMN_LATITUD + " REAL," +
                    COLUMN_LONGITUD + " REAL," +
                    COLUMN_CATEGORIA + " TEXT," + // Añadido
                    COLUMN_ESTADO_SYNC + " INTEGER DEFAULT 0)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_INCIDENCIAS;

    public EcoCityDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_INCIDENCIAS + " ADD COLUMN " + COLUMN_CATEGORIA + " TEXT");
        }
    }
    
    public long insertarIncidencia(Incidencia incidencia) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, incidencia.getTitulo());
        values.put(COLUMN_DESCRIPCION, incidencia.getDescripcion());
        values.put(COLUMN_CATEGORIA, incidencia.getCategoria());
        values.put(COLUMN_URGENCIA, incidencia.getUrgencia());
        values.put(COLUMN_FECHA, incidencia.getFecha());
        values.put(COLUMN_FOTO_URI, incidencia.getFotoUri());
        values.put(COLUMN_AUDIO_URI, incidencia.getAudioUri());
        values.put(COLUMN_LATITUD, incidencia.getLatitud());
        values.put(COLUMN_LONGITUD, incidencia.getLongitud());
        values.put(COLUMN_ESTADO_SYNC, incidencia.getEstadoSync());
        
        return db.insert(TABLE_INCIDENCIAS, null, values);
    }
    
    public List<Incidencia> obtenerTodasIncidencias() {
        List<Incidencia> incidencias = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_INCIDENCIAS, null, null, null, null, null, COLUMN_ID + " DESC");
        
        while (cursor.moveToNext()) {
            incidencias.add(cursorToIncidencia(cursor));
        }
        cursor.close();
        return incidencias;
    }
    
    public Incidencia obtenerIncidencia(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        Cursor cursor = db.query(TABLE_INCIDENCIAS, null, selection, selectionArgs, null, null, null);
        
        Incidencia incidencia = null;
        if (cursor.moveToFirst()) {
            incidencia = cursorToIncidencia(cursor);
        }
        cursor.close();
        return incidencia;
    }
    
    public int actualizarIncidencia(Incidencia incidencia) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, incidencia.getTitulo());
        values.put(COLUMN_DESCRIPCION, incidencia.getDescripcion());
        values.put(COLUMN_CATEGORIA, incidencia.getCategoria());
        values.put(COLUMN_URGENCIA, incidencia.getUrgencia());
        values.put(COLUMN_FOTO_URI, incidencia.getFotoUri());
        values.put(COLUMN_AUDIO_URI, incidencia.getAudioUri());
        values.put(COLUMN_LATITUD, incidencia.getLatitud());
        values.put(COLUMN_LONGITUD, incidencia.getLongitud());
        values.put(COLUMN_ESTADO_SYNC, incidencia.getEstadoSync());
        
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(incidencia.getId()) };
        
        return db.update(TABLE_INCIDENCIAS, values, selection, selectionArgs);
    }
    
    public int eliminarIncidencia(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        return db.delete(TABLE_INCIDENCIAS, selection, selectionArgs);
    }
    
    public int actualizarEstadoSync(int id, int estadoSync) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ESTADO_SYNC, estadoSync);
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        return db.update(TABLE_INCIDENCIAS, values, selection, selectionArgs);
    }
    
    public List<Incidencia> obtenerIncidenciasPendientes() {
        List<Incidencia> incidencias = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ESTADO_SYNC + " = ?";
        String[] selectionArgs = { "0" };
        Cursor cursor = db.query(TABLE_INCIDENCIAS, null, selection, selectionArgs, null, null, COLUMN_ID + " ASC");
        
        while (cursor.moveToNext()) {
            incidencias.add(cursorToIncidencia(cursor));
        }
        cursor.close();
        return incidencias;
    }
    
    /**
     * ¡MÉTODO AÑADIDO! Borra los datos locales y los reemplaza con los del servidor.
     */
    public void sincronizarDesdeFirestore(List<Incidencia> incidenciasDeFirestore) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Borrar solo las incidencias que ya están sincronizadas (para no perder las locales pendientes)
            String whereClause = COLUMN_ESTADO_SYNC + " = ?";
            String[] whereArgs = {"1"};
            db.delete(TABLE_INCIDENCIAS, whereClause, whereArgs);

            // 2. Insertar los datos "frescos" de Firestore
            for (Incidencia incidencia : incidenciasDeFirestore) {
                ContentValues values = new ContentValues();
                // El ID no se inserta, es autoincremental
                values.put(COLUMN_TITULO, incidencia.getTitulo());
                values.put(COLUMN_DESCRIPCION, incidencia.getDescripcion());
                values.put(COLUMN_CATEGORIA, incidencia.getCategoria());
                values.put(COLUMN_URGENCIA, incidencia.getUrgencia());
                values.put(COLUMN_FECHA, incidencia.getFecha());
                values.put(COLUMN_FOTO_URI, incidencia.getFotoUri());
                values.put(COLUMN_AUDIO_URI, incidencia.getAudioUri());
                values.put(COLUMN_LATITUD, incidencia.getLatitud());
                values.put(COLUMN_LONGITUD, incidencia.getLongitud());
                values.put(COLUMN_ESTADO_SYNC, 1); // Vienen de Firestore, están sincronizadas
                db.insert(TABLE_INCIDENCIAS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    
    private Incidencia cursorToIncidencia(Cursor cursor) {
        Incidencia incidencia = new Incidencia();
        incidencia.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        incidencia.setTitulo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITULO)));
        incidencia.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPCION)));
        incidencia.setCategoria(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA)));
        incidencia.setUrgencia(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URGENCIA)));
        incidencia.setFecha(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA)));
        incidencia.setFotoUri(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO_URI)));
        incidencia.setAudioUri(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUDIO_URI)));
        incidencia.setLatitud(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUD)));
        incidencia.setLongitud(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUD)));
        incidencia.setEstadoSync(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ESTADO_SYNC)));
        return incidencia;
    }
}
