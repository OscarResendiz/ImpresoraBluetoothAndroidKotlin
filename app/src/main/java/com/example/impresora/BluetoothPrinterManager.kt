package com.example.impresora

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // UUID estándar para SPP (Serial Port Profile)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        const val BUFFER_SIZE = 1024
        const val PRINT_DELAY = 100L // Milisegundos entre líneas
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return if (bluetoothAdapter != null && hasBluetoothPermission()) {
            bluetoothAdapter.bondedDevices.toList()
        } else {
            emptyList()
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-11
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 y anteriores
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, onResult: (success: Boolean, message: String) -> Unit) {
        scope.launch {
            try {
                if (!hasBluetoothPermission()) {
                    onResult(false, "Permiso de Bluetooth no concedido")
                    return@launch
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                isConnected = true

                withContext(Dispatchers.Main) {
                    onResult(true, "Conectado a: ${device.name}")
                }
            } catch (e: IOException) {
                isConnected = false
                withContext(Dispatchers.Main) {
                    onResult(false, "Error de conexión: ${e.message}")
                }
            }
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            isConnected = false
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendData(data: String, onResult: (success: Boolean, message: String) -> Unit) {
        if (!isConnected) {
            onResult(false, "No hay conexión Bluetooth activa")
            return
        }

        scope.launch {
            try {
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
                withContext(Dispatchers.Main) {
                    onResult(true, "Datos enviados exitosamente")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error al enviar: ${e.message}")
                }
            }
        }
    }

    fun sendBinaryData(data: ByteArray, onResult: (success: Boolean, message: String) -> Unit) {
        if (!isConnected) {
            onResult(false, "No hay conexión Bluetooth activa")
            return
        }

        scope.launch {
            try {
                outputStream?.write(data)
                outputStream?.flush()
                withContext(Dispatchers.Main) {
                    onResult(true, "Datos binarios enviados exitosamente")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error al enviar datos binarios: ${e.message}")
                }
            }
        }
    }

    fun printTicket(ticket: String, onResult: (success: Boolean, message: String) -> Unit) {
        if (!isConnected) {
            onResult(false, "No hay conexión Bluetooth activa")
            return
        }

        scope.launch {
            try {
                val lines = ticket.split("\n")
                for (line in lines) {
                    outputStream?.write((line + "\n").toByteArray())
                    outputStream?.flush()
                    delay(PRINT_DELAY)
                }
                // Enviar comandos de fin de impresión (feed)
                outputStream?.write("\n\n\n".toByteArray())
                outputStream?.flush()

                withContext(Dispatchers.Main) {
                    onResult(true, "Ticket impreso exitosamente")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error al imprimir: ${e.message}")
                }
            }
        }
    }

    fun printTicketWithImage(
        imageCommand: ByteArray,
        ticketText: String,
        qrCommand: ByteArray = byteArrayOf(),
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (!isConnected) {
            onResult(false, "No hay conexión Bluetooth activa")
            return
        }

        scope.launch {
            try {
                // Primero enviar el comando de imagen (GS v 0)
                if (imageCommand.isNotEmpty()) {
                    outputStream?.write(imageCommand)
                    outputStream?.flush()
                    delay(300L)  // Esperar a que procese la imagen
                }

                // Luego enviar el texto del ticket
                val lines = ticketText.split("\n")
                for (line in lines) {
                    outputStream?.write((line + "\n").toByteArray())
                    outputStream?.flush()
                    delay(PRINT_DELAY)
                }

                // Si hay comando de QR, enviarlo después del texto
                if (qrCommand.isNotEmpty()) {
                    outputStream?.write(qrCommand)
                    outputStream?.flush()
                    delay(200L)
                }

                // Enviar comandos de fin de impresión (feed)
                outputStream?.write("\n\n\n".toByteArray())
                outputStream?.flush()

                withContext(Dispatchers.Main) {
                    onResult(true, "Ticket con imagen impreso exitosamente")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error al imprimir: ${e.message}")
                }
            }
        }
    }

    fun getConnectionStatus(): Boolean = isConnected

    fun shutdown() {
        scope.cancel()
        disconnect()
    }
}

