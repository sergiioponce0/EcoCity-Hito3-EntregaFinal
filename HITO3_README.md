# Hito 3: La Nube y Servicios Conectados - CityCare

## Implementación de Arquitectura Híbrida Cloud + Sockets

Este documento describe la implementación completa del Hito 3 para CityCare, incluyendo Firebase (Cloud) y Sockets TCP (PSP).

---

## 1. Estructura del Proyecto

```
CityCare-main/
├── app/
│   ├── src/main/java/
│   │   ├── com/example/ecocity/
│   │   │   └── CityCareApplication.java    # Inicialización de Firebase
│   │   ├── controller/
│   │   │   └── IncidenciaController.java   # CRUD con sincronización
│   │   ├── model/
│   │   │   ├── EcoCityDbHelper.java        # SQLite + métodos de sync
│   │   │   └── Incidencia.java             # Modelo con estadoSync
│   │   ├── network/
│   │   │   └── SocketCliente.java          # Cliente TCP nativo (PSP)
│   │   ├── sync/
│   │   │   └── FirebaseSyncManager.java    # Sincronización Firestore
│   │   ├── utils/
│   │   │   └── NetworkUtils.java           # Detección de red
│   │   └── view/
│   │       ├── ChatActivity.java           # Chat con sockets
│   │       ├── LoginActivity.java          # Firebase Auth
│   │       └── MainActivity.java           # Menú principal
│   ├── src/main/res/layout/
│   │   ├── activity_chat.xml               # UI del chat
│   │   ├── item_mensaje_enviado.xml        # Burbuja mensaje enviado
│   │   ├── item_mensaje_recibido.xml       # Burbuja mensaje recibido
│   │   └── item_mensaje_sistema.xml        # Mensajes del sistema
│   ├── src/main/res/menu/
│   │   └── menu_main.xml                   # Menú con opciones
│   ├── src/main/res/drawable/
│   │   ├── bg_mensaje_enviado.xml          # Estilo burbuja enviada
│   │   ├── bg_mensaje_recibido.xml         # Estilo burbuja recibida
│   │   └── bg_input_mensaje.xml            # Estilo input
│   ├── google-services.json                # Config Firebase (template)
│   ├── build.gradle.kts                    # Dependencias Firebase
│   └── AndroidManifest.xml                 # Permisos y activities
├── CentroDeControl/
│   └── ServidorChat.java                   # Servidor TCP Java
├── build.gradle.kts                        # Plugin Firebase
└── HITO3_README.md                         # Este documento
```

---

## 2. Infraestructura Cloud (Firebase)

### 2.1 Firebase Authentication

**Archivo:** `view/LoginActivity.java`

Implementación real con Firebase Auth:
- Login con email y contraseña
- Registro de nuevos usuarios
- Verificación de sesión al iniciar
- Logout desde el menú principal

```java
// Inicializar Firebase Auth
mAuth = FirebaseAuth.getInstance();

// Login
mAuth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(...);

// Registro
mAuth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener(...);
```

### 2.2 Firebase Firestore

**Archivo:** `sync/FirebaseSyncManager.java`

Base de datos NoSQL con sincronización automática:
- Guardar incidencias en Firestore
- Obtener incidencias del usuario
- Sincronización de pendientes
- Callbacks para operaciones async

### 2.3 Detección de Red

**Archivo:** `utils/NetworkUtils.java`

Sistema para detectar estado de conectividad:
- Verificar disponibilidad de red
- Detectar tipo de conexión (WiFi/Datos)
- Compatible con todas las versiones de Android

```java
public static boolean isNetworkAvailable(Context context)
public static boolean isWifiConnected(Context context)
public static boolean isMobileConnected(Context context)
```

---

## 3. Chat de Soporte (Sockets - PSP)

**NOTA:** Este módulo NO utiliza Firebase. Implementación pura de sockets Java.

### 3.1 Cliente Socket TCP (Android)

**Archivo:** `network/SocketCliente.java`

Demuestra manejo de sockets nativos con:
- `java.net.Socket` - Conexión TCP
- `DataOutputStream` - Envío de datos
- `DataInputStream` - Recepción de datos
- Hilos concurrentes (`Thread`) para operaciones de red
- Comunicación asíncrona con UI mediante `Handler`

```java
// Crear socket
socket = new Socket(serverIp, serverPort);

// Streams de datos
dataInputStream = new DataInputStream(socket.getInputStream());
dataOutputStream = new DataOutputStream(socket.getOutputStream());

// Hilo de recepción
recepcionThread = new Thread(() -> {
    while (recibiendo) {
        String mensaje = dataInputStream.readUTF();
        // Procesar mensaje...
    }
});
```

### 3.2 Activity de Chat

**Archivo:** `view/ChatActivity.java`

Interfaz de chat con:
- Configuración de IP y puerto del servidor
- Indicador de estado de conexión
- Historial de mensajes
- Burbujas de mensajes (enviado/recibido/sistema)

### 3.3 Servidor Centro de Control

**Archivo:** `CentroDeControl/ServidorChat.java`

Aplicación de escritorio Java:
- `ServerSocket` para aceptar conexiones
- Múltiples clientes simultáneos (hilos)
- Consola de administración
- Comandos: /list, /msg, /broadcast, /kick, /stop

**Ejecutar servidor:**
```bash
cd CentroDeControl
javac ServidorChat.java
java ServidorChat [puerto]
```

Por defecto usa el puerto 5555.

---

## 4. Configuración de Firebase

### Paso 1: Crear proyecto en Firebase Console
1. Ir a https://console.firebase.google.com
2. Crear nuevo proyecto "CityCare-Hito3"
3. Agregar app Android con package: `com.example.ecocity`

### Paso 2: Descargar google-services.json
1. Descargar el archivo `google-services.json` desde Firebase Console
2. Reemplazar el archivo en: `app/google-services.json`

### Paso 3: Habilitar servicios
1. **Authentication** → Sign-in method → Habilitar "Email/Password"
2. **Firestore Database** → Crear base de datos → Modo "pruebas"

### Paso 4: Reglas de Firestore (Desarrollo)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 5. Cómo Ejecutar

### A. Servidor Centro de Control

```bash
cd CentroDeControl
javac ServidorChat.java
java ServidorChat 5555
```

Comandos disponibles:
- `/list` - Ver clientes conectados
- `/msg <usuario> <mensaje>` - Mensaje privado
- `/broadcast <mensaje>` - Mensaje a todos
- `/kick <usuario>` - Desconectar usuario
- `/stop` - Detener servidor

### B. Aplicación Android

1. Abrir proyecto en Android Studio
2. Sincronizar Gradle
3. Ejecutar en dispositivo/emulador
4. Login con cuenta Firebase (o crear una)
5. Acceder a Chat desde el menú (⋮)

### C. Probar Chat

1. Configurar IP del servidor en el chat
   - Usar IP de la máquina donde corre el servidor
   - Para emulador: usar `10.0.2.2`
2. Conectar
3. Enviar mensajes

---

## 6. Demostración Académica

### PMDM (Programación Multimedia y Dispositivos Móviles):
- ✅ Integración de Firebase en Android
- ✅ Autenticación de usuarios con Firebase Auth
- ✅ Base de datos NoSQL (Firestore)
- ✅ Operaciones asíncronas con callbacks
- ✅ Detección de estado de red
- ✅ Sincronización offline/online

### PSP (Programación de Servicios y Procesos):
- ✅ Comunicación TCP/IP con Sockets
- ✅ `DataInputStream` / `DataOutputStream`
- ✅ Hilos concurrentes para operaciones de red
- ✅ Servidor multi-cliente (uno por hilo)
- ✅ Protocolo de aplicación propio
- ✅ Gestión de estado de conexión
- ✅ Comunicación UI-hilos con Handler

---

## 7. Notas Importantes

### Firebase
- El archivo `google-services.json` incluido es un **TEMPLATE**
- Debe ser reemplazado por el archivo real de Firebase Console
- Sin este archivo, la app no funcionará correctamente

### Chat Socket
- El módulo de chat **NO usa Firebase**
- Implementación pura de sockets TCP nativos en Java
- Cumple el requisito crítico del Hito 3

### Sincronización
- Las incidencias se guardan primero en SQLite local
- Si hay red, se sincronizan automáticamente con Firestore
- Si no hay red, quedan marcadas como "pendientes"
- Al recuperar la conexión, se sincronizan las pendientes

---

## 8. Archivos Clave

| Archivo | Descripción |
|---------|-------------|
| `LoginActivity.java` | Autenticación Firebase |
| `FirebaseSyncManager.java` | Sincronización Firestore |
| `NetworkUtils.java` | Detección de red |
| `SocketCliente.java` | Cliente TCP nativo |
| `ChatActivity.java` | Interfaz de chat |
| `ServidorChat.java` | Servidor de escritorio |
| `EcoCityDbHelper.java` | SQLite con sync |

---

## Autor
Implementación para Hito 3 - Arquitectura Híbrida Cloud + Sockets
