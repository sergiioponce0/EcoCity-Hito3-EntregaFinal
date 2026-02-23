package network;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Cliente Socket TCP nativo para el Chat de Soporte.
 * Implementa comunicación con el Centro de Control (servidor de escritorio).
 * Usa DataInputStream, DataOutputStream y gestión de hilos concurrentes.
 */
public class SocketCliente {
    
    private static final String TAG = "SocketCliente";
    
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    
    private Thread recepcionThread;
    private boolean conectado = false;
    private boolean recibiendo = false;
    
    private OnMessageListener messageListener;
    private OnConnectionListener connectionListener;
    
    /**
     * Constructor del cliente socket
     * @param serverIp IP del Centro de Control (servidor)
     * @param serverPort Puerto del servidor
     */
    public SocketCliente(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
    
    /**
     * Establece el listener para mensajes recibidos
     */
    public void setOnMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * Establece el listener para eventos de conexión
     */
    public void setOnConnectionListener(OnConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    /**
     * Conecta al servidor del Centro de Control.
     * Se ejecuta en un hilo separado para no bloquear el UI.
     */
    public void conectar() {
        if (conectado) {
            Log.w(TAG, "Ya está conectado");
            return;
        }
        
        Thread conexionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Intentando conectar a " + serverIp + ":" + serverPort);
                    
                    // Crear socket y conectar al servidor
                    socket = new Socket(serverIp, serverPort);
                    
                    // Crear streams de entrada y salida
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    
                    conectado = true;
                    
                    Log.d(TAG, "Conexión establecida con el servidor");
                    
                    // Notificar conexión exitosa
                    if (connectionListener != null) {
                        connectionListener.onConnected();
                    }
                    
                    // Iniciar hilo de recepción de mensajes
                    iniciarRecepcion();
                    
                } catch (IOException e) {
                    Log.e(TAG, "Error al conectar: " + e.getMessage());
                    conectado = false;
                    if (connectionListener != null) {
                        connectionListener.onConnectionError(e.getMessage());
                    }
                }
            }
        });
        
        conexionThread.start();
    }
    
    /**
     * Inicia el hilo de recepción de mensajes del servidor.
     * Este hilo escucha constantemente mensajes entrantes.
     */
    private void iniciarRecepcion() {
        if (recibiendo) {
            return;
        }
        
        recibiendo = true;
        
        recepcionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Hilo de recepción iniciado");
                
                while (recibiendo && conectado) {
                    try {
                        // Leer mensaje UTF del servidor (bloqueante)
                        String mensaje = dataInputStream.readUTF();
                        
                        Log.d(TAG, "Mensaje recibido: " + mensaje);
                        
                        // Notificar al listener en el hilo principal
                        if (messageListener != null) {
                            messageListener.onMessageReceived(mensaje);
                        }
                        
                    } catch (IOException e) {
                        if (recibiendo) {
                            Log.e(TAG, "Error al recibir mensaje: " + e.getMessage());
                            recibiendo = false;
                            conectado = false;
                            
                            if (connectionListener != null) {
                                connectionListener.onDisconnected("Error de conexión: " + e.getMessage());
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Hilo de recepción finalizado");
            }
        });
        
        recepcionThread.start();
    }
    
    /**
     * Envía un mensaje de texto plano al servidor.
     * Se ejecuta en un hilo separado para no bloquear el UI.
     * @param mensaje Texto a enviar
     */
    public void enviarMensaje(final String mensaje) {
        if (!conectado || dataOutputStream == null) {
            Log.e(TAG, "No se puede enviar mensaje: no conectado");
            if (messageListener != null) {
                messageListener.onError("No conectado al servidor");
            }
            return;
        }
        
        Thread envioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Enviar mensaje como UTF
                    dataOutputStream.writeUTF(mensaje);
                    dataOutputStream.flush();
                    
                    Log.d(TAG, "Mensaje enviado: " + mensaje);
                    
                    // Notificar que el mensaje fue enviado
                    if (messageListener != null) {
                        messageListener.onMessageSent(mensaje);
                    }
                    
                } catch (IOException e) {
                    Log.e(TAG, "Error al enviar mensaje: " + e.getMessage());
                    if (messageListener != null) {
                        messageListener.onError("Error al enviar: " + e.getMessage());
                    }
                }
            }
        });
        
        envioThread.start();
    }
    
    /**
     * Desconecta del servidor y libera recursos.
     */
    public void desconectar() {
        Log.d(TAG, "Desconectando...");
        
        recibiendo = false;
        conectado = false;
        
        // Cerrar streams
        try {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar DataInputStream: " + e.getMessage());
        }
        
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar DataOutputStream: " + e.getMessage());
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar socket: " + e.getMessage());
        }
        
        dataInputStream = null;
        dataOutputStream = null;
        socket = null;
        
        Log.d(TAG, "Desconectado");
        
        if (connectionListener != null) {
            connectionListener.onDisconnected("Desconectado manualmente");
        }
    }
    
    /**
     * Verifica si está conectado al servidor
     */
    public boolean isConectado() {
        return conectado && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    /**
     * Obtiene la IP del servidor
     */
    public String getServerIp() {
        return serverIp;
    }
    
    /**
     * Obtiene el puerto del servidor
     */
    public int getServerPort() {
        return serverPort;
    }
    
    // Interfaces de callback
    
    /**
     * Listener para mensajes enviados y recibidos
     */
    public interface OnMessageListener {
        void onMessageReceived(String mensaje);
        void onMessageSent(String mensaje);
        void onError(String error);
    }
    
    /**
     * Listener para eventos de conexión
     */
    public interface OnConnectionListener {
        void onConnected();
        void onDisconnected(String razon);
        void onConnectionError(String error);
    }
}
