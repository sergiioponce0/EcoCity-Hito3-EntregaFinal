# Hito 3: La Nube y Servicios Conectados

## Implementación de Arquitectura Híbrida Cloud + Sockets

Este documento describe la implementación completa del Hito 3 para CityCare, incluyendo Firebase (Cloud) y Sockets TCP (PSP).

---

## 1. Infraestructura Cloud (Firebase - PMDM)

### 1.1 Firebase Authentication

Se ha implementado autenticación real con Firebase:

- **Clase:** `network/FirebaseAuthManager.java`
- **Funcionalidades:**
  - Login con email y contraseña
  - Registro de nuevos usuarios
  - Gestión de sesión (logout, estado)
  - Callbacks para éxito/error

### 1.2 Firebase Firestore

Base de datos NoSQL en la nube para sincronización:

- **Clase:** `network/FirestoreManager.java`
- **Funcionalidades:**
  - Subir incidencias a Firestore
  - Obtener incidencias del usuario
  - Sincronización masiva de pendientes
  - Callbacks para operaciones async

### 1.3 Detección de Red

Sistema para detectar estado de conectividad:

- **Clase:** `network/NetworkDetector.java`
- **Funcionalidades:**
  - Verificar disponibilidad de red
  - Detectar tipo de conexión (WiFi/Datos)
  - Listener para cambios de red (API 24+)
  - Compatible con todas las versiones de Android

---

## 2. Chat de Soporte (Sockets - PSP)

**NOTA IMPORTANTE:** Este módulo NO utiliza Firebase. Implementación pura de sockets Java.

### 2.1 Cliente Socket TCP (Android)

Demuestra manejo de sockets nativos:

- **Clase:** `network/ChatClient.java`
- **Conceptos PSP demostrados:**
  - `java.net.Socket` - Conexión TCP
  - `DataOutputStream` - Envío de datos
  - `DataInputStream` - Recepción de datos
  - Hilos concurrentes (`Thread`) para operaciones de red
  - Comunicación asíncrona con UI mediante `Handler`

- **Protocolo de mensajes:**
  - `[LOGIN]<nombre>` - Registro de usuario
  - `[MSG]<nombre>: <mensaje>` - Mensaje de chat
  - `[LOGOUT]<nombre>` - Desconexión
  - `[SYSTEM]<mensaje>` - Mensaje del sistema

### 2.2 Activity de Chat

- **Clase:** `view/ChatActivity.java`
- **Características:**
  - Interfaz de chat tipo WhatsApp
  - Configuración de IP y puerto del servidor
  - Indicador de estado de conexión
  - Historial de mensajes con RecyclerView

### 2.3 Servidor Centro de Control (Java Desktop)

Aplicación de escritorio que actúa como servidor:

- **Archivo:** `CentroDeControl/ServidorChat.java`
- **Características:**
  - `ServerSocket` para aceptar conexiones
  - Múltiples clientes simultáneos (hilos)
  - Consola de administración
  - Comandos: /list, /msg, /broadcast, /kick, /stop

---

## 3. Configuración de Firebase

### Paso 1: Crear proyecto en Firebase Console
1. Ir a https://console.firebase.google.com
2. Crear nuevo proyecto "CityCare-Hito3"
3. Agregar app Android con package: `com.example.ecocity`

### Paso 2: Descargar google-services.json
1. Descargar el archivo `google-services.json`
2. Colocarlo en: `app/google-services.json` (reemplazar el existente)

### Paso 3: Habilitar servicios
1. En Firebase Console → Authentication → Sign-in method
2. Habilitar "Email/Password"
3. En Firebase Console → Firestore Database
4. Crear base de datos en modo "producción" o "pruebas"

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

## 4. Cómo Ejecutar

### A. Servidor Centro de Control

```bash
cd CentroDeControl
javac ServidorChat.java
java ServidorChat [puerto]
```

Por defecto usa el puerto 5555.

Comandos disponibles en el servidor:
- `/list` - Ver clientes conectados
- `/msg <usuario> <mensaje>` - Mensaje privado
- `/broadcast <mensaje>` - Mensaje a todos
- `/kick <usuario>` - Desconectar usuario
- `/stop` - Detener servidor

### B. Aplicación Android

1. Sincronizar proyecto con Gradle
2. Ejecutar en dispositivo/emulador
3. Login con cuenta Firebase (o crear una)
4. Acceder a Chat desde el menú

### C. Probar Chat

1. Configurar IP del servidor en el chat (botón ⚙️)
2. Usar la IP de la máquina donde corre el servidor
3. Conectar (botón 🔍)
4. Enviar mensajes

---

## 5. Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                        ANDROID APP                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Firebase    │  │  Firestore   │  │   ChatClient     │  │
│  │   Auth       │  │   Manager    │  │  (Socket TCP)    │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Network    │  │  Incidencia  │  │    ChatActivity  │  │
│  │   Detector   │  │  Controller  │  │                  │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
┌─────────────────────┐        ┌─────────────────────┐
│      FIREBASE       │        │   CENTRO CONTROL    │
│  ┌───────────────┐  │        │   ┌─────────────┐   │
│  │ Authentication│  │        │   │ ServidorChat│   │
│  └───────────────┘  │        │   │  (Java)     │   │
│  ┌───────────────┐  │        │   └─────────────┘   │
│  │   Firestore   │  │        │        TCP          │
│  │   Database    │  │        │       :5555         │
│  └───────────────┘  │        └─────────────────────┘
└─────────────────────┘
```

---

## 6. Archivos Creados/Modificados

### Nuevos archivos Java:
- `network/FirebaseAuthManager.java`
- `network/FirestoreManager.java`
- `network/NetworkDetector.java`
- `network/ChatClient.java`
- `view/ChatActivity.java`
- `view/ChatAdapter.java`
- `CentroDeControl/ServidorChat.java`

### Archivos modificados:
- `view/LoginActivity.java` - Auth real con Firebase
- `view/MainActivity.java` - Menú con chat y sync
- `controller/IncidenciaController.java` - Métodos de sync
- `build.gradle` (proyecto y app) - Dependencias Firebase

### Nuevos layouts:
- `layout/activity_chat.xml`
- `layout/item_chat_sent.xml`
- `layout/item_chat_received.xml`
- `layout/item_chat_system.xml`
- `layout/dialog_chat_settings.xml`
- `layout/activity_login.xml` (actualizado)
- `menu/menu_main.xml`

### Nuevos drawables:
- `drawable/bg_edittext.xml`
- `drawable/bg_chat_sent.xml`
- `drawable/bg_chat_received.xml`
- `drawable/bg_chat_system.xml`

### Configuración:
- `AndroidManifest.xml` - Permisos de red
- `google-services.json` - Config Firebase (plantilla)
- `settings.gradle` - Repositorios
- `proguard-rules.pro` - Reglas ProGuard

---

## 7. Demostración Académica

Este proyecto demuestra:

### PMDM (Programación Multimedia y de Dispositivos Móviles):
- Integración de Firebase en Android
- Autenticación de usuarios
- Base de datos NoSQL (Firestore)
- Operaciones asíncronas con callbacks
- Gestión de permisos de red
- SharedPreferences para configuración

### PSP (Programación de Servicios y Procesos):
- Comunicación TCP/IP con Sockets
- DataInputStream / DataOutputStream
- Hilos concurrentes para operaciones de red
- Servidor multi-cliente (uno por hilo)
- Protocolo de aplicación propio
- Gestión de estado de conexión

---

## Notas para el Profesor

1. **Firebase:** El proyecto está configurado para usar Firebase. El archivo `google-services.json` incluido es una plantilla y debe ser reemplazado por uno real de Firebase Console.

2. **Chat Socket:** El módulo de chat utiliza sockets TCP nativos sin ninguna dependencia de Firebase, cumpliendo el requisito crítico.

3. **Servidor:** El `ServidorChat.java` puede ejecutarse desde cualquier máquina en la misma red. Para pruebas en emulador, usar `10.0.2.2` como IP.

4. **Sincronización:** La lógica de sincronización detecta automáticamente cuando hay red y sube las incidencias pendientes a Firestore.
