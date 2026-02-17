package com.example.impresora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Impresora de imágenes simplificada para impresoras térmicas
 * Genera un encabezado visual simple y nada complicado
 */
object SimpleImagePrinter {

    /**
     * Genera un encabezado decorativo simple
     */
    fun getLogoHeader(): String {
        return """
            ================================
            *      [L O G O T I P O]      *
            ================================
        """.trimIndent() + "\n"
    }

    /**
     * Intenta crear ASCII art muy simple (experimental)
     * Si falla, retorna el encabezado por defecto
     */
    fun tryConvertToASCII(context: Context): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logotipo)
            val w = 32
            val h = (bitmap.height.toFloat() / bitmap.width * w).toInt().coerceIn(4, 12)
            val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
            
            val chars = "XOXO*+=-:."
            val sb = StringBuilder()
            
            for (y in 0 until scaled.height) {
                for (x in 0 until scaled.width) {
                    val pixel = scaled.getPixel(x, y)
                    val brightness = ((pixel shr 16 and 0xFF) + 
                                     (pixel shr 8 and 0xFF) + 
                                     (pixel and 0xFF)) / 3
                    val idx = (brightness * chars.length / 256).coerceIn(0, chars.length - 1)
                    sb.append(chars[idx])
                }
                sb.append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            getLogoHeader()
        }
    }
}
