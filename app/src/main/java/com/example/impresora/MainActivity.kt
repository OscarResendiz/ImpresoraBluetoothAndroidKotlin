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
        updatePreview(TicketGenerator.generateSampleTicket())
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
        updatePreview(ticket)
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

        updatePreview(testMessage)
        showMessage("Enviando prueba de conexión...", Toast.LENGTH_SHORT)

        bluetoothManager.printTicket(testMessage) { success, message ->
            showMessage(message, Toast.LENGTH_SHORT)
        }
    }

    private fun printTicketWithImage() {
        if (!bluetoothManager.getConnectionStatus()) {
            showMessage("Primero debes conectar una impresora Bluetooth", Toast.LENGTH_SHORT)
            return
        }

        showMessage("Generando ticket con imagen...", Toast.LENGTH_SHORT)

        try {
            // Generar ticket con imagen
            val ticketData = TicketGenerator.generateSampleTicketWithImage(this)
            
            // Actualizar preview con el texto
            updatePreview(ticketData.textContent)
            
            // Si hay comando de imagen, enviarlo
            if (ticketData.imageCommand.isNotEmpty()) {
                showMessage("Enviando ticket con imagen a imprimir...", Toast.LENGTH_SHORT)
                bluetoothManager.printTicketWithImage(
                    ticketData.imageCommand,
                    ticketData.textContent,
                    ticketData.qrCommand
                ) { success, message ->
                    showMessage(message, Toast.LENGTH_SHORT)
                }
            } else {
                // Si no hay imagen, imprimir solo texto
                showMessage("Advertencia: No hay imagen. Imprimiendo solo texto...", Toast.LENGTH_LONG)
                bluetoothManager.printTicket(ticketData.textContent) { success, message ->
                    showMessage(message, Toast.LENGTH_SHORT)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showMessage("Error: ${e.message}", Toast.LENGTH_SHORT)
        }
    }

    private fun updateStatus(status: String, isConnected: Boolean) {
        binding.statusText.text = status
        binding.statusText.setTextColor(
            if (isConnected) android.graphics.Color.GREEN else android.graphics.Color.RED
        )
    }

    private fun updatePreview(text: String) {
        binding.previewText.text = text
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
}
