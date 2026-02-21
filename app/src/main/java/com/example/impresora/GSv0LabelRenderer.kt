package com.example.impresora

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Componente para interpretar comandos GS v 0 (GS+0) de impresoras térmicas
 * y generar una imagen visual de la etiqueta.
 */
class GSv0LabelRenderer {
    /**
     * Interpreta un comando GS v 0 y genera una imagen Bitmap
     * @param command ByteArray con el comando GS v 0
     * @param width Ancho de la etiqueta en pixels
     * @param height Alto de la etiqueta en pixels
     * @return Bitmap generado a partir del comando
     */
    fun renderLabel(command: ByteArray, width: Int, height: Int): Bitmap {
        // GS v 0 formato: GS (0x1D) v (0x76) 0 (0x30) m xL xH yL yH [imageData]
        // Buscar cabecera y extraer datos
        if (command.size < 8 || command[0] != 0x1D.toByte() || command[1] != 0x76.toByte()) {
            throw IllegalArgumentException("Comando GS v 0 inválido")
        }
        val m = command[3].toInt()
        val xL = command[4].toInt() and 0xFF
        val xH = command[5].toInt() and 0xFF
        val yL = command[6].toInt() and 0xFF
        val yH = command[7].toInt() and 0xFF
        val imgWidthBytes = xL + (xH shl 8)
        val imgHeight = yL + (yH shl 8)
        val imgData = command.copyOfRange(8, command.size)

        // Crear bitmap
        val bitmap = Bitmap.createBitmap(imgWidthBytes * 8, imgHeight, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.color = Color.BLACK

        var dataIndex = 0
        for (y in 0 until imgHeight) {
            for (xByte in 0 until imgWidthBytes) {
                val byte = imgData[dataIndex++].toInt() and 0xFF
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    val isBlack = (byte and (0x80 shr bit)) != 0
                    bitmap.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
                }
            }
        }
        // Escalar a tamaño deseado si es necesario
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Renderiza la etiqueta completa: imagen GS v 0 + texto
     * @param command ByteArray con el comando GS v 0
     * @param text Texto del ticket
     * @param width Ancho de la etiqueta en pixels
     * @param height Alto de la etiqueta en pixels
     * @return Bitmap generado a partir del comando y texto
     */
    fun renderLabelWithText(command: ByteArray, text: String, width: Int, height: Int): Bitmap {
        // Renderizar imagen GS v 0
        val imgBitmap = renderLabel(command, width, height / 2)
        // Crear bitmap final
        val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawColor(Color.WHITE)
        // Dibujar imagen en la parte superior
        canvas.drawBitmap(imgBitmap, 0f, 0f, null)
        // Dibujar texto debajo
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 22f
        paint.isAntiAlias = true
        paint.typeface = android.graphics.Typeface.MONOSPACE
        val lines = text.split("\n")
        val startY = height / 2 + 30
        val lineHeight = 26f
        for ((i, line) in lines.withIndex()) {
            canvas.drawText(line, 10f, startY + i * lineHeight.toFloat(), paint)
        }
        return finalBitmap
    }
}
