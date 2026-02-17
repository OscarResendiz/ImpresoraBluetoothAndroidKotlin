# Aplicación de Impresora Bluetooth para Android

## 📋 Descripción
Esta aplicación permite conectar y imprimir en impresoras térmicas Bluetooth. Incluye funcionalidad completa para:
- Detectar y conectar a impresoras Bluetooth emparejadas
- Generar tickets de compra con formato profesional
- Enviar comandos de impresión a la impresora
- Vista previa en tiempo real del contenido a imprimir

## 🚀 Características Principales

### 1. **Conexión Bluetooth**
- Búsqueda automática de dispositivos Bluetooth emparejados
- Conexión segura mediante UUID estándar (SPP - Serial Port Profile)
- Manejo de permisos de Bluetooth para Android 6.0+
- Indica el estado de conexión en tiempo real

### 2. **Generación de Tickets**
- Ticket de ejemplo predefinido
- Generador personalizado de tickets con:
  - Nombre y dirección de la tienda
  - Listado de artículos con precios
  - Cálculo automático de subtotal, IVA y total
  - Fecha y hora de la compra
  - Numero de transacción personalizable

### 3. **Impresión**
- Envío de datos línea por línea con velocidad controlada
- Manejo de errores de conexión
- Feedback en tiempo real sobre el estado de impresión

## 📱 Requisitos del Sistema

### Versión de Android
- **API mínima:** 24 (Android 7.0)
- **API objetivo:** 36 (Android 15.0)

### Permisos Requeridos
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

### Hardware
- Dispositivo Android con Bluetooth
- Impresora térmica Bluetooth compatible con protocolo SPP

## 🛠️ Estructura del Proyecto

```
app/src/main/
├── java/com/example/impresora/
│   ├── MainActivity.kt              # Actividad principal
│   ├── BluetoothPrinterManager.kt  # Gestor de Bluetooth e impresión
│   └── TicketGenerator.kt          # Generador de tickets
└── res/layout/
    └── activity_main.xml            # Interfaz de usuario
```

### Clases Principales

#### **BluetoothPrinterManager**
Gestiona toda la comunicación Bluetooth:
```kotlin
// Conectar a un dispositivo
bluetoothManager.connectToDevice(device) { success, message ->
    // Manejar resultado
}

// Imprimir un ticket
bluetoothManager.printTicket(ticketContent) { success, message ->
    // Manejar resultado
}

// Obtener dispositivos pareados
val devices = bluetoothManager.getPairedDevices()

// Desconectar
bluetoothManager.disconnect()
```

#### **TicketGenerator**
Genera tickets formateados:
```kotlin
// Ticket de ejemplo
val ticket = TicketGenerator.generateSampleTicket()

// Ticket personalizado
val items = listOf(
    "Café Premium" to 5.50,
    "Donut Chocolate" to 2.50,
    "Agua Mineral" to 1.50
)
val customTicket = TicketGenerator.generateCustomTicket(
    storeName = "Tienda XYZ",
    storeAddress = "Calle Principal 123",
    items = items,
    taxPercentage = 0.19
)
```

## 📲 Cómo Usar la Aplicación

### Paso 1: Emparejar Impresora
1. Ve a Configuración > Bluetooth en tu dispositivo Android
2. Busca tu impresora Bluetooth
3. Empareja el dispositivo

### Paso 2: Ejecutar la Aplicación
1. Abre la aplicación
2. Verás una lista de dispositivos Bluetooth emparejados en el spinner

### Paso 3: Conectar a la Impresora
1. Selecciona la impresora de la lista
2. Toca el botón "Conectar"
3. Espera a que aparezca "Conectado"

### Paso 4: Imprimir
1. Elige una opción de impresión:
   - **"Imprimir Ticket de Ejemplo"**: Imprime un ticket de ejemplo
   - **"Imprimir Prueba de Conexión"**: Imprime un mensaje de prueba
2. La vista previa muestra el contenido antes de imprimir
3. Verifica la salida en la impresora

### Paso 5: Desconectar
1. Toca el botón "Desconectar" cuando termines

## 🔧 Desarrollo y Personalización

### Agregar Nuevos Tipos de Tickets
```kotlin
// En TicketGenerator.kt
fun generateInvoiceTicket(invoiceNumber: String, customerName: String, ...): String {
    // Implementar tu formato personalizado
}
```

### Modificar Velocidad de Impresión
En `BluetoothPrinterManager.kt`:
```kotlin
companion object {
    const val PRINT_DELAY = 100L // Aumentar para impresoras lentas
}
```

### Agregar Caracteres Especiales
Las impresoras térmicas soportan caracteres especiales:
```kotlin
val ticket = """
    ╔═══════════════════╗
    ║   ENCABEZADO      ║
    ╚═══════════════════╝
""".trimIndent()
```

## 🐛 Solución de Problemas

### "No hay dispositivos Bluetooth emparejados"
- Asegúrate de que tu dispositivo tiene Bluetooth habilitado
- Empareja la impresora primero en Configuración > Bluetooth

### "Error de conexión"
- Verifica que la impresora está encendida
- Asegúrate de que está a rango (típicamente 10 metros)
- Intenta "Desconectar" y luego "Conectar" nuevamente
- Revisa los permisos de Bluetooth en Configuración > Aplicaciones

### "Nada se imprime"
- Verifica que hay papel en la impresora
- Comprueba que está seleccionada la impresora correcta
- Intenta primero la "Prueba de Conexión"
- Algunos modelos pueden requerir caracteres especiales adicionales

### "Permisos denegados"
- La aplicación solicitará permisos en el primer inicio
- Si se deniegan, ve a Configuración > Aplicaciones > Permutadora > Permisos
- Habilita todos los permisos de Bluetooth

## 📝 Formatos de Ticket por Defecto

### Ancho de Impresión
La mayoría de impresoras térmicas aceptan 32-40 caracteres por línea. El formato de ejemplo usa 32 caracteres.

### Configuración de Márgenes
```kotlin
val separador = "================================" // 32 caracteres
val linea = "--------------------------------"   // 32 caracteres
```

## 🔐 Seguridad y Permisos

La aplicación solicita explícitamente:
- `BLUETOOTH`: Detectar y conectar a dispositivos
- `BLUETOOTH_ADMIN`: Acceso de administrador (heredado)
- `BLUETOOTH_SCAN`: Escanear dispositivos (Android 12+)
- `BLUETOOTH_CONNECT`: Conectar a dispositivos emparejados (Android 12+)

Todos los permisos son necesarios para el funcionamiento correcto.

## 🚀 Compilación y Ejecución

### Compilar
```bash
./gradlew build
```

### Ejecutar en dispositivo
```bash
./gradlew installDebug
```

### Ejecutar tests
```bash
./gradlew test
```

## 📚 Librerías Utilizadas

- **AndroidX Core KTX**: Utilidades de Android
- **Material Design**: Componentes de IU
- **Kotlin Coroutines**: Programación asincrónica
- **Android Bluetooth API**: Conectividad Bluetooth nativa

## 📄 Licencia

Este proyecto es de código abierto y está disponible para uso educativo y comercial.

## 👨‍💻 Autor

Proyecto de demostración para impresión térmica Bluetooth en Android Kotlin.

## 📞 Soporte

Para problemas con impresoras específicas:
1. Consulta el manual de tu impresora
2. Verifica que soporta protocolo SPP (Serial Port Profile)
3. Algunos modelos pueden requerir ajustes en los comandos de impresión

---

**Última actualización:** Febrero 2026
**Versión:** 1.0
