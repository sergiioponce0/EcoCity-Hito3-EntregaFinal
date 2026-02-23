package model; // Revisa tu paquete

public class Incidencia {
    private int id;
    private String titulo;
    private String descripcion;
    private String urgencia;
    private String fecha;
    private String categoria;

    // Campos para Hito 2 y 3 (los dejamos listos pero pueden ser null al principio)
    private String fotoUri;
    private String audioUri;
    private double latitud;
    private double longitud;
    private int estadoSync; // 0 = pendiente, 1 = sincronizado

    // Constructor vacío (necesario para Firebase más adelante)
    public Incidencia() {
    }

    // Constructor para crear una incidencia nueva (sin ID, porque es autoincremental)
    public Incidencia(String titulo, String descripcion, String urgencia, String fecha) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.urgencia = urgencia;
        this.fecha = fecha;
        this.estadoSync = 0; // Por defecto no está sincronizada
    }

    // Constructor completo (útil cuando leemos de la BBDD)
    public Incidencia(int id, String titulo, String descripcion, String urgencia, String fecha, String fotoUri, String audioUri, double latitud, double longitud, int estadoSync) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.urgencia = urgencia;
        this.fecha = fecha;
        this.fotoUri = fotoUri;
        this.audioUri = audioUri;
        this.latitud = latitud;
        this.longitud = longitud;
        this.estadoSync = estadoSync;
    }

    // Getters y Setters (Imprescindibles)
    // Truco: En Android Studio: Click Derecho -> Generate -> Getter and Setter -> Selecciona todos

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUrgencia() { return urgencia; }
    public void setUrgencia(String urgencia) { this.urgencia = urgencia; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getFotoUri() { return fotoUri; }
    public void setFotoUri(String fotoUri) { this.fotoUri = fotoUri; }

    public String getAudioUri() { return audioUri; }
    public void setAudioUri(String audioUri) { this.audioUri = audioUri; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public int getEstadoSync() { return estadoSync; }
    public void setEstadoSync(int estadoSync) { this.estadoSync = estadoSync; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}