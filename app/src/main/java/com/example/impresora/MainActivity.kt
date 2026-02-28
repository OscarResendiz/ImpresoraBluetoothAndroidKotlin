package com.example.impresora

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.impresora.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothPrinterManager
    private var connectedDevice: BluetoothDevice? = null
    private val pairedDevices = mutableListOf<BluetoothDevice>()
    private val getBluetoothPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-11
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 9 y anteriores
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Data binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Bluetooth Manager
        bluetoothManager = BluetoothPrinterManager(this)

        // Solicitar permisos
        requestBluetoothPermissions()

        // Configurar listeners
        setupListeners()

        // Cargar dispositivos pareados
        loadPairedDevices()

        // Mostrar preview inicial del ticket
        //updatePreview(TicketGenerator.generateSampleTicket())
        showLabelPreview()
    }

    private fun requestBluetoothPermissions() {
        val permissions = getBluetoothPermissions
        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        } else {
            // Todos los permisos ya están otorgados
            loadPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        pairedDevices.clear()
        val devices = bluetoothManager.getPairedDevices()
        pairedDevices.addAll(devices)

        val deviceNames = devices.map { it.name ?: "Dispositivo desconocido" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.deviceSpinner.adapter = adapter

        if (devices.isEmpty()) {
            showMessage("No hay dispositivos Bluetooth pareados", Toast.LENGTH_LONG)
        }
    }

    private fun setupListeners() {
        binding.connectButton.setOnClickListener {
            connectToDevice()
        }

        binding.disconnectButton.setOnClickListener {
            disconnectDevice()
        }

        binding.printSampleButton.setOnClickListener {
            printSampleTicket()
        }

        binding.printWithImageButton.setOnClickListener {
            printTicketWithImage()
        }

        binding.testPrintButton.setOnClickListener {
            printTestMessage()
        }

        binding.openDrawerButton.setOnClickListener {
            openCashDrawer()
        }

        binding.previewLabelButton.setOnClickListener {
            showLabelPreview()
        }

        binding.sendWhatsAppButton.setOnClickListener {
            sendTicketViaWhatsApp()
        }
    }

    private fun connectToDevice() {
        val selectedIndex = binding.deviceSpinner.selectedItemPosition

        if (selectedIndex < 0 || selectedIndex >= pairedDevices.size) {
            showMessage("Selecciona un dispositivo Bluetooth")
            return
        }

        connectedDevice = pairedDevices[selectedIndex]
        showMessage("Conectando a ${connectedDevice?.name}...", Toast.LENGTH_SHORT)

        bluetoothManager.connectToDevice(connectedDevice!!) { success, message ->
            if (success) {
                updateStatus("Conectado: ${connectedDevice?.name}", true)
                showMessage(message, Toast.LENGTH_SHORT)
            } else {
                updateStatus("Error de conexión", false)
                showMessage(message, Toast.LENGTH_SHORT)
            }
        }
    }

    private fun disconnectDevice() {
        bluetoothManager.disconnect()
        updateStatus("Desconectado", false)
        showMessage("Desconectado de la impresora", Toast.LENGTH_SHORT)
    }

    private fun printSampleTicket() {
        if (!bluetoothManager.getConnectionStatus()) {
            showMessage("Primero debes conectar una impresora Bluetooth", Toast.LENGTH_SHORT)
            return
        }

        val ticket = TicketGenerator.generateSampleTicket()
        updatePreview(ticket, null, null)
        showMessage("Enviando ticket a imprimir...", Toast.LENGTH_SHORT)

        bluetoothManager.printTicket(ticket) { success, message ->
            showMessage(message, Toast.LENGTH_SHORT)
        }
    }

    private fun printTestMessage() {
        if (!bluetoothManager.getConnectionStatus()) {
            showMessage("Primero debes conectar una impresora Bluetooth", Toast.LENGTH_SHORT)
            return
        }

        val testMessage = """
            ================================
            PRUEBA DE CONEXIÓN BLUETOOTH
            ================================
            
            Este es un mensaje de prueba.
            Si ves esto en la impresora,
            la conexión funciona correctamente.
            
            Hora: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}
            
            ================================
        """.trimIndent()

        updatePreview(testMessage, null, null)
        showMessage("Enviando prueba de conexión...", Toast.LENGTH_SHORT)

        bluetoothManager.printTicket(testMessage) { success, message ->
            showMessage(message, Toast.LENGTH_SHORT)
        }
    }

    private fun openCashDrawer() {
        if (!bluetoothManager.getConnectionStatus()) {
            showMessage("Primero debes conectar una impresora Bluetooth", Toast.LENGTH_SHORT)
            return
        }

        showMessage("Enviando comando al cajón de dinero...", Toast.LENGTH_SHORT)
        bluetoothManager.openCashDrawer { _, message ->
            showMessage(message, Toast.LENGTH_SHORT)
        }
    }

    private fun printTicketWithImage() {
        // Solo imprimir si el usuario confirma
        if (!bluetoothManager.getConnectionStatus()) {
            showMessage("Primero debes conectar una impresora Bluetooth", Toast.LENGTH_SHORT)
            return
        }
        showMessage("Enviando ticket a imprimir...", Toast.LENGTH_SHORT)
        try {
            val ticketData = TicketGenerator.generateSampleTicketWithImage(this)
            if (ticketData.imageCommand.isNotEmpty()) {
                bluetoothManager.printTicketWithImage(
                    ticketData.imageCommand,
                    ticketData.textContent,
                    ticketData.qrCommand
                ) { success, message ->
                    showMessage(message, Toast.LENGTH_SHORT)
                }
            } else {
                bluetoothManager.printTicket(ticketData.textContent) { success, message ->
                    showMessage(message, Toast.LENGTH_SHORT)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showMessage("Error: ${e.message}", Toast.LENGTH_SHORT)
        }
    }

    private fun sendTicketViaWhatsApp() {
        val items = listOf(
            "Cafe Premium" to 5.50,
            "Donut Chocolate" to 2.50,
            "Agua Mineral" to 1.50,
            "Pan Integral" to 3.00,
            "Zumo Natural" to 4.00
        )
        val ticketData = TicketGenerator.generateCustomTicketWithImage(
            this,
            storeName = "TIENDA EJEMPLO",
            storeAddress = "Bloque Centro",
            items = items,
            taxPercentage = 0.19
        )
        val gsRenderer = GSv0LabelRenderer()
        val lineCount = ticketData.textContent.split("\n").size
        val width = 600
        val height = 60 + lineCount * 48
        
        val labelBitmap = if (ticketData.imageCommand.isNotEmpty()) {
            try {
                gsRenderer.renderLabelWithText(ticketData.imageCommand, ticketData.textContent, width, height)
            } catch (e: Exception) {
                null
            }
        } else null
        
        val qrBitmap = QRCodeGenerator.generateQRCode(ticketData.ticketId, size = 640)

        if (labelBitmap == null || qrBitmap == null) {
            showMessage("No se pudo generar la imagen del ticket")
            return
        }

        val combinedWidth = java.lang.Math.max(labelBitmap.width, qrBitmap.width)
        val combinedHeight = labelBitmap.height + qrBitmap.height + 40
        
        val combinedBitmap = android.graphics.Bitmap.createBitmap(combinedWidth, combinedHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(combinedBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        val labelLeft = (combinedWidth - labelBitmap.width) / 2f
        canvas.drawBitmap(labelBitmap, labelLeft, 0f, null)
        
        val qrLeft = (combinedWidth - qrBitmap.width) / 2f
        canvas.drawBitmap(qrBitmap, qrLeft, labelBitmap.height.toFloat() + 20f, null)

        try {
            val cachePath = java.io.File(externalCacheDir, "shared_images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "ticket.png")
            val stream = java.io.FileOutputStream(file)
            combinedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.setPackage("com.whatsapp")
            intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val genericIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
                genericIntent.type = "image/png"
                genericIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                genericIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(android.content.Intent.createChooser(genericIntent, "Compartir ticket"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showMessage("Error al guardar la imagen para compartir")
        }
    }

    private fun updateStatus(status: String, isConnected: Boolean) {
        binding.statusText.text = status
        binding.statusText.setTextColor(
            if (isConnected) android.graphics.Color.GREEN else android.graphics.Color.RED
        )
    }

    private fun updatePreview(text: String, image: android.graphics.Bitmap? = null, qr: android.graphics.Bitmap? = null) {
        //binding.previewText.text = text

        // Mostrar imagen si existe
        if (image != null) {
            binding.previewImage.setImageBitmap(image)
            binding.previewImage.visibility = android.view.View.VISIBLE
        } else {
            binding.previewImage.visibility = android.view.View.GONE
        }

        // Mostrar QR si existe

        if (qr != null) {
            binding.previewQr.setImageBitmap(qr)
            binding.previewQr.visibility = android.view.View.VISIBLE
        } else {
            binding.previewQr.visibility = android.view.View.GONE
        }


    }

    private fun showMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allPermissionsGranted) {
                showMessage("Se requieren permisos de Bluetooth para usar esta aplicación", Toast.LENGTH_LONG)
            }
        }
    }

    override fun onDestroy() {
        bluetoothManager.shutdown()
        super.onDestroy()
    }

    private fun showLabelPreview() {
        // Generar ticket con imagen y datos completos
        val items = listOf(
            "Cafe Premium" to 5.50,
            "Donut Chocolate" to 2.50,
            "Agua Mineral" to 1.50,
            "Pan Integral" to 3.00,
            "Zumo Natural" to 4.00
        )
        val ticketData = TicketGenerator.generateCustomTicketWithImage(
            this,
            storeName = "TIENDA EJEMPLO",
            storeAddress = "Bloque Centro",
            items = items,
            taxPercentage = 0.19
        )
        val gsRenderer = GSv0LabelRenderer()
        // Calcular altura dinámica según cantidad de líneas (factor mayor)
        val lineCount = ticketData.textContent.split("\n").size
        val width = 600 // Aumenta el ancho del bitmap
        val height = 60 + lineCount * 48
        val labelBitmap = if (ticketData.imageCommand.isNotEmpty()) {
            try {
                gsRenderer.renderLabelWithText(ticketData.imageCommand, ticketData.textContent, width, height)
            } catch (e: Exception) {
                null
            }
        } else null
        val qrBitmap = QRCodeGenerator.generateQRCode(ticketData.ticketId, size = 640) // Aumenta el tamaño del QR
        updatePreview("", labelBitmap, qrBitmap)
        showMessage("Vista previa generada. Revisa antes de imprimir.", Toast.LENGTH_SHORT)
    }
}
