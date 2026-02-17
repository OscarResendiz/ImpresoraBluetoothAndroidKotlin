package com.example.impresora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.res.ResourcesCompat

object ImageToASCII {

    /**
     * Convierte una imagen Bitmap a ASCII art para imprimir en impresora térmica
     * @param bitmap La imagen a convertir
     * @param width El ancho máximo en caracteres (típicamente 32-48 para impresoras térmicas)
     * @return String con el ASCII art
     */
    fun convertBitmapToASCII(bitmap: Bitmap, width: Int = 40): String {
        // Escalar el bitmap a un tamaño manejable
        val aspectRatio = bitmap.height.toFloat() / bitmap.width
        val height = (width * aspectRatio).toInt().coerceAtLeast(1)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        
        val ascii = StringBuilder()
        
        for (y in 0 until resizedBitmap.height) {
            for (x in 0 until resizedBitmap.width) {
                val pixel = resizedBitmap.getPixel(x, y)
                val brightness = calculateBrightness(pixel)
                ascii.append(getASCIIChar(brightness))
            }
            ascii.append("\n")
        }
        
        return ascii.toString()
    }

    /**
     * Convierte la imagen del drawable a ASCII art
     * @param context Contexto de la aplicación
     * @param drawableId ID del recurso drawable
     * @param width Ancho máximo en caracteres
     * @return String con el ASCII art
     */
    fun convertDrawableToASCII(context: Context, drawableId: Int, width: Int = 40): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
            convertBitmapToASCII(bitmap, width)
        } catch (e: Exception) {
            e.printStackTrace()
            "" // Retorna vacío si hay error
        }
    }


    /**
     * Calcula el brillo de un pixel (0-255)
     */
    private fun calculateBrightness(pixel: Int): Int {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Fórmula estándar de brillo percibido
        return (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
    }

    /**
     * Retorna el carácter ASCII correspondiente al brillo
     * Usa solo caracteres ASCII estándar compatibles con todas las impresoras
     */
    private fun getASCIIChar(brightness: Int): Char {
        return when {
            brightness < 26 -> '@'   // Negro sólido
            brightness < 51 -> '#'   // Negro oscuro
            brightness < 77 -> '%'   // Gris oscuro
            brightness < 102 -> '*'  // Gris medio-oscuro
            brightness < 128 -> '+'  // Gris medio
            brightness < 153 -> '='  // Gris medio-claro
            brightness < 179 -> '-'  // Gris claro
            brightness < 204 -> ':'  // Muy claro
            brightness < 230 -> '.'  // Casi blanco
            else -> ' '              // Espacio (blanco)
        }
    }

    /**
     * Genera una versión centrada y enmarcada del ASCII art
     */
    fun centerASCIIArt(asciiArt: String, frameWidth: Int = 40): String {
        val lines = asciiArt.split("\n")
        val centered = StringBuilder()
        
        for (line in lines) {
            val trimmedLine = line.trimEnd()
            if (trimmedLine.isNotEmpty()) {
                val padding = (frameWidth - trimmedLine.length) / 2
                if (padding > 0) {
                    centered.append(" ".repeat(padding))
                }
                centered.append(trimmedLine.take(frameWidth))
            }
            centered.append("\n")
        }
        
        return centered.toString()
    }
}
