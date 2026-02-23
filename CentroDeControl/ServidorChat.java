import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Centro de Control - Servidor de Chat TCP
 * 
 * Aplicación de escritorio que actúa como servidor para el chat de soporte.
 * Recibe conexiones de clientes Android y permite la comunicación bidireccional.
 * 
 * Objetivo Académico: Demostrar manejo de ServerSocket, sockets cliente,
 * DataInputStream, DataOutputStream y gestión de hilos concurrentes.
 */
public class ServidorChat {
    
    private static final int DEFAULT_PORT = 5555;
    private static final String VERSION = "1.0";
    
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    
    // Lista de clientes conectados
    private List<ClientHandler> connectedClients;
    
    public ServidorChat(int port) {
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }
    
    /**
     * Iniciar el servidor
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            System.out.println("========================================");
            System.out.println("  CENTRO DE CONTROL - Servidor Chat v" + VERSION);
            System.out.println("========================================");
            System.out.println("Servidor iniciado en puerto: " + port);
            System.out.println("Esperando conexiones de clientes...");
            System.out.println("----------------------------------------");
            
            // Hilo para aceptar conexiones entrantes
            Thread acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running) {
                        try {
                            // Aceptar nueva conexión
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("[+] Nueva conexión desde: " + 
                                clientSocket.getInetAddress().getHostAddress());
                            
                            // Crear handler para el cliente
                            ClientHandler handler = new ClientHandler(clientSocket, ServidorChat.this);
                            connectedClients.add(handler);
                            handler.start();
                            
                        } catch (IOException e) {
                            if (running) {
                                System.err.println("[!] Error al aceptar conexión: " + e.getMessage());
                            }
                        }
                    }
                }
            });
            
            acceptThread.start();
            
            // Consola de administración
            runConsole();
            
        } catch (IOException e) {
            System.err.println("[!] Error al iniciar servidor: " + e.getMessage());
        }
    }
    
    /**
     * Consola de administración del servidor
     */
    private void runConsole() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\nComandos disponibles:");
        System.out.println("  /list    - Listar clientes conectados");
        System.out.println("  /msg <cliente> <mensaje> - Enviar mensaje a cliente específico");
        System.out.println("  /broadcast <mensaje> - Enviar mensaje a todos");
        System.out.println("  /kick <cliente> - Desconectar cliente");
        System.out.println("  /stop    - Detener servidor");
        System.out.println("  /help    - Mostrar ayuda");
        System.out.println();
        
        while (running) {
            System.out.print("[CentroControl]> ");
            String command = scanner.nextLine().trim();
            
            if (command.isEmpty()) continue;
            
            if (command.equals("/stop")) {
                stop();
                break;
            } else if (command.equals("/list")) {
                listClients();
            } else if (command.equals("/help")) {
                showHelp();
            } else if (command.startsWith("/broadcast ")) {
                String message = command.substring(11);
                broadcastMessage("[SYSTEM]" + message, null);
                System.out.println("[Sistema] Mensaje broadcast enviado");
            } else if (command.startsWith("/msg ")) {
                String[] parts = command.split(" ", 3);
                if (parts.length >= 3) {
                    sendToClient(parts[1], "[SYSTEM]" + parts[2]);
                } else {
                    System.out.println("[!] Uso: /msg <cliente> <mensaje>");
                }
            } else if (command.startsWith("/kick ")) {
                String clientName = command.substring(6);
                kickClient(clientName);
            } else {
                System.out.println("[!] Comando desconocido. Use /help para ver comandos.");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Listar clientes conectados
     */
    private void listClients() {
        System.out.println("\n--- Clientes conectados ---");
        if (connectedClients.isEmpty()) {
            System.out.println("No hay clientes conectados");
        } else {
            for (int i = 0; i < connectedClients.size(); i++) {
                ClientHandler client = connectedClients.get(i);
                System.out.println((i + 1) + ". " + client.getClientName() + 
                    " (" + client.getIpAddress() + ")");
            }
        }
        System.out.println("---------------------------\n");
    }
    
    /**
     * Enviar mensaje a cliente específico
     */
    private void sendToClient(String clientName, String message) {
        for (ClientHandler client : connectedClients) {
            if (client.getClientName().equalsIgnoreCase(clientName)) {
                client.sendMessage(message);
                System.out.println("[Sistema] Mensaje enviado a " + clientName);
                return;
            }
        }
        System.out.println("[!] Cliente no encontrado: " + clientName);
    }
    
    /**
     * Desconectar cliente
     */
    private void kickClient(String clientName) {
        for (ClientHandler client : connectedClients) {
            if (client.getClientName().equalsIgnoreCase(clientName)) {
                client.sendMessage("[SYSTEM]Has sido desconectado por el administrador");
                client.disconnect();
                System.out.println("[Sistema] Cliente desconectado: " + clientName);
                return;
            }
        }
        System.out.println("[!] Cliente no encontrado: " + clientName);
    }
    
    /**
     * Mostrar ayuda
     */
    private void showHelp() {
        System.out.println("\n--- COMANDOS DISPONIBLES ---");
        System.out.println("  /list              - Listar clientes conectados");
        System.out.println("  /msg <c> <m>       - Enviar mensaje a cliente");
        System.out.println("  /broadcast <m>     - Enviar a todos");
        System.out.println("  /kick <cliente>    - Desconectar cliente");
        System.out.println("  /stop              - Detener servidor");
        System.out.println("  /help              - Mostrar esta ayuda");
        System.out.println("----------------------------\n");
    }
    
    /**
     * Detener servidor
     */
    public void stop() {
        running = false;
        System.out.println("\n[*] Deteniendo servidor...");
        
        // Desconectar todos los clientes
        for (ClientHandler client : new ArrayList<>(connectedClients)) {
            client.disconnect();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[!] Error al cerrar servidor: " + e.getMessage());
        }
        
        System.out.println("[*] Servidor detenido");
    }
    
    /**
     * Enviar mensaje a todos los clientes
     */
    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    /**
     * Remover cliente de la lista
     */
    public void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        System.out.println("[-] Cliente desconectado: " + client.getClientName());
    }
    
    /**
     * Verificar si nombre de cliente ya existe
     */
    public boolean isClientNameTaken(String name) {
        for (ClientHandler client : connectedClients) {
            if (client.getClientName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Permitir especificar puerto como argumento
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[!] Puerto inválido, usando puerto por defecto: " + DEFAULT_PORT);
            }
        }
        
        ServidorChat servidor = new ServidorChat(port);
        servidor.start();
    }
}

/**
 * Handler para cada cliente conectado
 * Gestiona la comunicación con un cliente específico en un hilo separado
 */
class ClientHandler extends Thread {
    
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private ServidorChat servidor;
    
    private String clientName;
    private boolean connected = false;
    
    public ClientHandler(Socket socket, ServidorChat servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }
    
    @Override
    public void run() {
        try {
            // Inicializar streams
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            connected = true;
            
            // Enviar mensaje de bienvenida
            sendMessage("[SYSTEM]Bienvenido al Centro de Control de CityCare");
            
            // Bucle principal de recepción de mensajes
            while (connected) {
                try {
                    // Leer mensaje del cliente
                    String message = inputStream.readUTF();
                    processMessage(message);
                    
                } catch (IOException e) {
                    if (connected) {
                        System.out.println("[!] Error con cliente " + clientName + ": " + e.getMessage());
                    }
                    break;
                }
            }
            
        } catch (IOException e) {
            System.err.println("[!] Error al inicializar streams: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * Procesar mensaje recibido del cliente
     */
    private void processMessage(String message) {
        if (message.startsWith("[LOGIN]")) {
            // Registro de cliente
            clientName = message.substring(7);
            
            // Verificar si nombre ya existe
            if (servidor.isClientNameTaken(clientName) && servidor.connectedClients.size() > 1) {
                sendMessage("[SYSTEM]Error: Nombre de usuario ya en uso");
                disconnect();
                return;
            }
            
            System.out.println("[+] Cliente registrado: " + clientName);
            sendMessage("[SYSTEM]Conexión exitosa. Bienvenido, " + clientName + "!");
            servidor.broadcastMessage("[SYSTEM]" + clientName + " se ha conectado", this);
            
        } else if (message.startsWith("[MSG]")) {
            // Mensaje de chat
            String chatMessage = message.substring(5);
            System.out.println("[" + clientName + "]: " + chatMessage);
            
            // Reenviar a otros clientes (incluyendo soporte/admin)
            servidor.broadcastMessage(message, this);
            
        } else if (message.startsWith("[LOGOUT]")) {
            // Desconexión voluntaria
            System.out.println("[-] Cliente desconectado: " + clientName);
            servidor.broadcastMessage("[SYSTEM]" + clientName + " se ha desconectado", this);
            disconnect();
            
        } else {
            // Mensaje genérico
            System.out.println("[" + clientName + "]: " + message);
        }
    }
    
    /**
     * Enviar mensaje al cliente
     */
    public void sendMessage(String message) {
        try {
            if (outputStream != null && connected) {
                outputStream.writeUTF(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            System.err.println("[!] Error al enviar mensaje a " + clientName + ": " + e.getMessage());
        }
    }
    
    /**
     * Desconectar cliente
     */
    public void disconnect() {
        connected = false;
        
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[!] Error al cerrar conexión: " + e.getMessage());
        }
        
        servidor.removeClient(this);
    }
    
    public String getClientName() {
        return clientName != null ? clientName : "Desconocido";
    }
    
    public String getIpAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}
