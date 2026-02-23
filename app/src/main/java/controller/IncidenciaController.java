package controller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import model.EcoCityDbHelper;
import model.Incidencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de Incidencias
 * Gestiona operaciones CRUD en SQLite con soporte para sincronización
 */
public class IncidenciaController {

    private EcoCityDbHelper dbHelper;
    private Context context;

    public IncidenciaController(Context context) {
        this.context = context;
        this.dbHelper = new EcoCityDbHelper(context);
    }

    // --- CREATE: Guardar una nueva incidencia ---
    public long crearIncidencia(Incidencia incidencia) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(EcoCityDbHelper.COLUMN_TITULO, incidencia.getTitulo());
        values.put(EcoCityDbHelper.COLUMN_DESCRIPCION, incidencia.getDescripcion());
        values.put(EcoCityDbHelper.COLUMN_URGENCIA, incidencia.getUrgencia());
        values.put(EcoCityDbHelper.COLUMN_FECHA, incidencia.getFecha());
        values.put(EcoCityDbHelper.COLUMN_FOTO_URI, incidencia.getFotoUri());
        values.put(EcoCityDbHelper.COLUMN_AUDIO_URI, incidencia.getAudioUri());
        values.put(EcoCityDbHelper.COLUMN_LATITUD, incidencia.getLatitud());
        values.put(EcoCityDbHelper.COLUMN_LONGITUD, incidencia.getLongitud());
        values.put(EcoCityDbHelper.COLUMN_ESTADO_SYNC, 0); // 0 = pendiente de sincronizar

        long newRowId = db.insert(EcoCityDbHelper.TABLE_INCIDENCIAS, null, values);
        db.close();
        return newRowId;
    }

    // --- READ: Obtener todas las incidencias ---
    public List<Incidencia> obtenerTodasLasIncidencias() {
        List<Incidencia> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                EcoCityDbHelper.TABLE_INCIDENCIAS,
                null, null, null, null, null,
                EcoCityDbHelper.COLUMN_ID + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Incidencia inc = cursorToIncidencia(cursor);
                lista.add(inc);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }
    
    // --- READ: Obtener solo incidencias pendientes de sincronizar ---
    public List<Incidencia> obtenerIncidenciasPendientes() {
        List<Incidencia> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = EcoCityDbHelper.COLUMN_ESTADO_SYNC + " = ?";
        String[] selectionArgs = { "0" };

        Cursor cursor = db.query(
                EcoCityDbHelper.TABLE_INCIDENCIAS,
                null, selection, selectionArgs, null, null,
                EcoCityDbHelper.COLUMN_ID + " ASC"
        );

        if (cursor.moveToFirst()) {
            do {
                Incidencia inc = cursorToIncidencia(cursor);
                lista.add(inc);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }
    
    // --- UPDATE: Marcar incidencia como sincronizada ---
    public int marcarSincronizada(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(EcoCityDbHelper.COLUMN_ESTADO_SYNC, 1); // 1 = sincronizada
        
        String selection = EcoCityDbHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        
        int count = db.update(
                EcoCityDbHelper.TABLE_INCIDENCIAS,
                values,
                selection,
                selectionArgs
        );
        
        db.close();
        return count;
    }
    
    // --- DELETE: Eliminar incidencia ---
    public int eliminarIncidencia(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String selection = EcoCityDbHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        
        int deletedRows = db.delete(EcoCityDbHelper.TABLE_INCIDENCIAS, selection, selectionArgs);
        db.close();
        return deletedRows;
    }

    // --- READ: Obtener una incidencia por id ---
    public Incidencia obtenerIncidencia(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = EcoCityDbHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = db.query(
                EcoCityDbHelper.TABLE_INCIDENCIAS,
                null,
                selection, selectionArgs,
                null, null, null
        );

        Incidencia inc = null;
        if (cursor.moveToFirst()) {
            inc = cursorToIncidencia(cursor);
        }
        cursor.close();
        db.close();
        return inc;
    }

    // --- UPDATE: Modificar una incidencia existente ---
    public int actualizarIncidencia(Incidencia incidencia) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(EcoCityDbHelper.COLUMN_TITULO, incidencia.getTitulo());
        values.put(EcoCityDbHelper.COLUMN_DESCRIPCION, incidencia.getDescripcion());
        values.put(EcoCityDbHelper.COLUMN_URGENCIA, incidencia.getUrgencia());
        values.put(EcoCityDbHelper.COLUMN_FECHA, incidencia.getFecha());
        values.put(EcoCityDbHelper.COLUMN_FOTO_URI, incidencia.getFotoUri());
        values.put(EcoCityDbHelper.COLUMN_AUDIO_URI, incidencia.getAudioUri());
        values.put(EcoCityDbHelper.COLUMN_LATITUD, incidencia.getLatitud());
        values.put(EcoCityDbHelper.COLUMN_LONGITUD, incidencia.getLongitud());
        values.put(EcoCityDbHelper.COLUMN_ESTADO_SYNC, incidencia.getEstadoSync());

        String selection = EcoCityDbHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(incidencia.getId()) };

        int count = db.update(EcoCityDbHelper.TABLE_INCIDENCIAS, values, selection, selectionArgs);
        db.close();
        return count;
    }
    
    // --- HELPER: Convertir cursor a objeto Incidencia ---
    private Incidencia cursorToIncidencia(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_ID));
        String titulo = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_TITULO));
        String desc = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_DESCRIPCION));
        String urgencia = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_URGENCIA));
        String fecha = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_FECHA));
        String foto = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_FOTO_URI));
        String audio = cursor.getString(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_AUDIO_URI));
        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_LATITUD));
        double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_LONGITUD));
        int sync = cursor.getInt(cursor.getColumnIndexOrThrow(EcoCityDbHelper.COLUMN_ESTADO_SYNC));

        return new Incidencia(id, titulo, desc, urgencia, fecha, foto, audio, lat, lon, sync);
    }
}
