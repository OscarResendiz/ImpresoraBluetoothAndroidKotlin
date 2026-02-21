package com.example.impresora

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object TicketGenerator {

    /**
     * Clase data que contiene el ticket con sus componentes
     */


    /**
     * Clase data que contiene el ticket con sus componentes
     */
    data class TicketData(
        val ticketId: String = "",                    // UUID único del ticket
        val imageCommand: ByteArray = byteArrayOf(),  // Comando GS v 0 con la imagen
        val qrCommand: ByteArray = byteArrayOf(),     // Comando GS v 0 con el QR
        val textContent: String = ""                   // Contenido de texto del ticket
    )

    fun generateSampleTicket(context: Any? = null): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "ES"))
        val currentDate = dateFormat.format(Date())

        return """
    ================================
           TIENDA EJEMPLO
             Bloque Centro
    ================================

    Fecha: $currentDate
    Ticket: 001234
    Caja: 1

    --------------------------------
    ARTICULO           CANT PRECIO
    --------------------------------

    Cafe Premium          2  5.50
    Donut Chocolate       3  2.50
    Agua Mineral          1  1.50
    Pan Integral          1  3.00
    Zumo Natural          2  4.00

    --------------------------------
    SUBTOTAL:                 24.50
    IVA 19%:                   4.66
    --------------------------------
    TOTAL:                    29.16
    --------------------------------

    Forma de pago: TARJETA DEBITO
    Transaccion: 1234567890

    Gracias por su compra!
    Vuelva pronto

    www.tiendaejemplo.com
    Tel: +1 234 567 8900

    ================================
            """.trimIndent()
    }

    fun generateCustomTicket(
        storeName: String,
        storeAddress: String,
        items: List<Pair<String, Double>>,
        taxPercentage: Double = 0.19,
        context: Any? = null
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "ES"))
        val currentDate = dateFormat.format(Date())

        val subtotal = items.sumOf { it.second }
        val tax = subtotal * taxPercentage
        val total = subtotal + tax

        var ticket = """
    ================================
    $storeName
    $storeAddress
    ================================

    Fecha: $currentDate
    Ticket: ${String.format("%06d", (Math.random() * 1000000).toInt())}

    --------------------------------
    ARTICULO              PRECIO
    --------------------------------

            """.trimIndent() + "\n"

        items.forEach { (itemName, price) ->
            ticket += String.format("%-30s %.2f\n", itemName, price)
        }

        ticket += """
    --------------------------------
    SUBTOTAL:               ${String.format("%.2f", subtotal)}
    IVA (${(taxPercentage * 100).toInt()}%%):                    ${String.format("%.2f", tax)}
    --------------------------------
    TOTAL:                  ${String.format("%.2f", total)}
    --------------------------------

    Gracias por su compra!
    Vuelva pronto

    ================================
            """.trimIndent()

        return ticket
    }

    /**
     * Genera un ticket de ejemplo con imagen y código QR usando GS v 0
     */
    fun generateSampleTicketWithImage(context: Context): TicketData {
        val ticketId = UUID.randomUUID().toString()

        // Obtener comando de imagen
        val imageCommand = try {
            ImageToRaster.drawableToGSv0Command(context, R.drawable.logotipo)
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf()
        }

        // Generar QR con el ID del ticket
        val qrBitmap = QRCodeGenerator.generateQRCode(ticketId)
        val qrCommand = if (qrBitmap != null) {
            try {
                // Usar 2/3 del ancho del ticket para el QR
                val qrWidth = (ImageToRaster.PRINTER_WIDTH_PIXELS * 2) / 3
                ImageToRaster.bitmapToGSv0Command(qrBitmap, qrWidth)
            } catch (e: Exception) {
                e.printStackTrace()
                byteArrayOf()
            }
        } else {
            byteArrayOf()
        }

        // Generar texto del ticket con el ID
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "ES"))
        val currentDate = dateFormat.format(Date())

        val textContent = """
    ================================
            TIENDA EJEMPLO
             Bloque Centro
    ================================

    Fecha: $currentDate
    Ticket: ${ticketId.take(8).uppercase()}
    Caja: 112345678912345678913456123
    --------------------------------
    ARTICULO           CANT PRECIO
    --------------------------------

    Cafe Premium          2  5.50
    Donut Chocolate       3  2.50
    Agua Mineral          1  1.50
    Pan Integral          1  3.00
    Zumo Natural          2  4.00

    --------------------------------
    SUBTOTAL:                 24.50
    IVA 19%:                   4.66
    --------------------------------
    TOTAL:                    29.16
    --------------------------------

    Forma de pago: TARJETA DEBITO
    Transaccion: $ticketId

    Codigo QR: [Ver debajo]

    Gracias por su compra!
    Vuelva pronto

    www.tiendaejemplo.com
    Tel: +1 234 567 8900

    ================================
            """.trimIndent()

        return TicketData(
            ticketId = ticketId,
            imageCommand = imageCommand,
            qrCommand = qrCommand,
            textContent = textContent
        )
    }

    /**
     * Genera un ticket personalizado con imagen y código QR usando GS v 0
     */
    fun generateCustomTicketWithImage(
        context: Context,
        storeName: String,
        storeAddress: String,
        items: List<Pair<String, Double>>,
        taxPercentage: Double = 0.19
    ): TicketData {
        val ticketId = UUID.randomUUID().toString()

        // Obtener comando de imagen
        val imageCommand = try {
            ImageToRaster.drawableToGSv0Command(context, R.drawable.logotipo)
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf()
        }

        // Generar QR con el ID del ticket
        val qrBitmap = QRCodeGenerator.generateQRCode(ticketId)
        val qrCommand = if (qrBitmap != null) {
            try {
                val qrWidth = (ImageToRaster.PRINTER_WIDTH_PIXELS * 2) / 3
                ImageToRaster.bitmapToGSv0Command(qrBitmap, qrWidth)
            } catch (e: Exception) {
                e.printStackTrace()
                byteArrayOf()
            }
        } else {
            byteArrayOf()
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "ES"))
        val currentDate = dateFormat.format(Date())

        val subtotal = items.sumOf { it.second }
        val tax = subtotal * taxPercentage
        val total = subtotal + tax

        var textContent = """
    ================================
    $storeName
    $storeAddress
    ================================

    Fecha: $currentDate
    Ticket: ${ticketId.take(8).uppercase()}

    --------------------------------
    ARTICULO              PRECIO
    --------------------------------

            """.trimIndent() + "\n"

        items.forEach { (itemName, price) ->
            textContent += String.format("%-30s %.2f\n", itemName, price)
        }

        textContent += """
    --------------------------------
    SUBTOTAL:               ${String.format("%.2f", subtotal)}
    IVA (${(taxPercentage * 100).toInt()}%%):                    ${String.format("%.2f", tax)}
    --------------------------------
    TOTAL:                  ${String.format("%.2f", total)}
    --------------------------------

    ID Transaccion: $ticketId

    Codigo QR: [Ver debajo]

    Gracias por su compra!
    Vuelva pronto

    ================================
            """.trimIndent()

        return TicketData(
            ticketId = ticketId,
            imageCommand = imageCommand,
            qrCommand = qrCommand,
            textContent = textContent
        )
    }
}
