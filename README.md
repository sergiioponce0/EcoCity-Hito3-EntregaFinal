# EcoCity: Tu Ciudad Sostenible 🌿

**EcoCity** es una aplicación móvil nativa diseñada para la gestión ciudadana de incidencias urbanas. Permite a los usuarios reportar problemas en la vía pública como baches, fallos de iluminación o residuos, integrando herramientas multimedia y una arquitectura robusta para fomentar la participación ciudadana.

---

## ⚠️ Advertencias de Configuración Técnica

Para que el proyecto compile y funcione correctamente, el usuario que descargue el repositorio debe realizar los siguientes ajustes manuales:

1.  **Versión del JDK**: Es obligatorio cambiar manualmente la versión del JDK en el entorno de desarrollo (Android Studio) para que coincida con la requerida por el proyecto. Esto es crítico debido al uso de **Sockets TCP nativos** de Java para el chat de soporte y la gestión de flujos de datos concurrentes.
2.  **Migración del Gradle Daemon**: Es necesario realizar la migración del **Gradle Daemon** para asegurar la compatibilidad con la estructura de construcción y las dependencias de red del sistema. Una configuración incorrecta puede provocar fallos en la sincronización de archivos y procesos multihilo.

---

## 🛠️ Especificaciones Técnicas

La aplicación está construida bajo estándares avanzados de desarrollo móvil:

* **Arquitectura MVC**: El código sigue el patrón Modelo - Vista - Controlador para garantizar un sistema escalable y fácil de mantener.
* **Filosofía Offline First**: Implementa persistencia local mediante **SQLite**, permitiendo registrar incidencias sin conexión a internet.
* **Sincronización Inteligente**: Utiliza un sistema de estados (`estadoSync`) para sincronizar automáticamente los datos locales con **Firebase Firestore** al recuperar la conexión.
* **Comunicaciones a Bajo Nivel**: Chat de soporte técnico mediante Sockets TCP nativos, utilizando hilos secundarios para evitar el bloqueo de la interfaz gráfica.
* **Seguridad Android 11+**: Uso de `FileProvider` para la generación de URIs seguras al capturar fotografías y gestionar archivos multimedia.



---

## 📱 Funcionalidades Principales

* **Registro de Incidencias**: Formulario con título, descripción y categorías (Vías, Residuos, Iluminación).
* **Multimedia y Ubicación**: Botones para tomar fotos, seleccionar archivos de galería, grabar audio y obtener la ubicación GPS.
* **Gamificación (Eco-Points)**: Sistema que otorga puntos por cada reporte y establece objetivos semanales para el ciudadano.
* **Acceso Seguro**: Autenticación de usuarios mediante **Firebase Auth** y soporte para inicio de sesión con Google.

---

## 👥 Equipo de Desarrollo (GETIMPROVE)

* **Antonio Jiménez**: Director de Proyecto.
* **Sergio Ponce Castro**: Director Creativo.
* **Virgilio J. Domínguez**: Jefe de Programación.

---

**EcoCity** - Mejorando el entorno con cada reporte.
