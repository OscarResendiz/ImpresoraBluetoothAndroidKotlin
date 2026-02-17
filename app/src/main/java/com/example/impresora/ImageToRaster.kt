package com.example.impresora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color

/**
 * Convierte imágenes a formato ESC/POS "GS v 0" (Raster Bit Image)
 * Estándar para impresoras térmicas
 */
object ImageToRaster {

    // Ajuste por defecto: usar 512 pixels (64 bytes) - común en impresoras de 58/50mm
    // Reducido desde 576px para evitar recortes en papeles más angostos
    const val PRINTER_WIDTH_BYTES = 64  // 512 pixels / 8
    const val PRINTER_WIDTH_PIXELS = PRINTER_WIDTH_BYTES * 8
    const val INTERNAL_PRINTER_WIDTH_BYTES = 48  // 512 pixels / 8
    const val INTERNAL_PRINTER_WIDTH_PIXELS = INTERNAL_PRINTER_WIDTH_BYTES * 8

    /**
     * Convierte una imagen a comando ESC/POS "GS v 0"
     * @param bitmap Imagen a convertir
     * @return ByteArray con el comando completo listo para enviar a la impresora
     */
    fun bitmapToGSv0Command(bitmap: Bitmap, targetWidth: Int = INTERNAL_PRINTER_WIDTH_PIXELS): ByteArray {
        // Redimensionar y convertir a blanco y negro (no estirar imágenes pequeñas)
        val processedBitmap = processBitmap(bitmap, targetWidth)

        // Obtener datos rasterizados
        val rasterData = getRasterData(processedBitmap)

        // Construir comando GS v 0
        return buildGSv0Command(rasterData, processedBitmap.height)
    }

    /**
     * Convierte drawable a comando GS v 0
     */
    fun drawableToGSv0Command(context: Context, drawableId: Int, targetWidth: Int = PRINTER_WIDTH_PIXELS): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
            bitmapToGSv0Command(bitmap, targetWidth)
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf() // Retorna vacío si falla
        }
    }

    /**
     * Procesa el bitmap: redimensiona y convierte a blanco y negro
     */
    private fun processBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
        // No estirar imagenes pequeñas: si la imagen es más pequeña que targetWidth, la centra
        val scaleToWidth = if (bitmap.width > targetWidth) targetWidth else bitmap.width
        val aspectRatio = bitmap.height.toFloat() / bitmap.width
        val newHeight = (scaleToWidth * aspectRatio).toInt().coerceIn(10, 400)

        // Redimensionar a la anchura calculada
        val resized = Bitmap.createScaledBitmap(bitmap, scaleToWidth, newHeight, true)

        // Si la imagen es más angosta que targetWidth, centrar en fondo blanco
        val workingBitmap = if (scaleToWidth < targetWidth) {
            val centered = Bitmap.createBitmap(targetWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(centered)
            canvas.drawColor(Color.WHITE)
            val left = ((targetWidth - resized.width) / 2).toFloat()
            canvas.drawBitmap(resized, left, 0f, null)
            centered
        } else {
            resized
        }

        // Convertir a blanco y negro
        val bwBitmap = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.RGB_565)
        val canvas2 = Canvas(bwBitmap)

        for (y in 0 until workingBitmap.height) {
            for (x in 0 until workingBitmap.width) {
                val pixel = workingBitmap.getPixel(x, y)
                val brightness = calculateBrightness(pixel)

                // Umbral: si es más claro que 128, es blanco; si no, es negro
                val bwColor = if (brightness > 128) Color.WHITE else Color.BLACK
                bwBitmap.setPixel(x, y, bwColor)
            }
        }

        return bwBitmap
    }

    /**
     * Calcula el brillo de un pixel (0-255)
     */
    private fun calculateBrightness(pixel: Int): Int {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
    }

    /**
     * Convierte el bitmap a datos rasterizados (bytes)
     * Cada byte representa 8 pixels horizontales (1 bit por pixel)
     * MSB a la izquierda
     */
    private fun getRasterData(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        
        // Recalcular ancho en bytes (debe ser múltiplo de 8)
        val widthBytes = (width + 7) / 8
        val data = ByteArray(widthBytes * height)
        
        var dataIndex = 0
        
        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byte: Int = 0
                
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        
                        // Si es negro, poner el bit a 1; si es blanco, poner a 0
                        if (pixel and 0xFFFFFF == 0) {  // Negro
                            byte = byte or (0x80 shr bit)
                        }
                    }
                }
                
                data[dataIndex++] = byte.toByte()
            }
        }
        
        return data
    }

    /**
     * Construye el comando ESC/POS "GS v 0"
     * Formato: GS v 0 m xL xH yL yH [imageData]
     * 
     * GS (29) v (118) 0 (48) = comando
     * m = 0 (modo normal)
     * xL xH = ancho en bytes (little-endian)
     * yL yH = altura en pixels (little-endian)
     */
    private fun buildGSv0Command(imageData: ByteArray, height: Int): ByteArray {
        val widthBytes = (imageData.size / height).coerceAtLeast(1)
        
        // Comando: GS (0x1D) v (0x76) 0 (0x30)
        val command = ByteArray(8 + imageData.size)
        var index = 0
        
        command[index++] = 0x1D  // GS
        command[index++] = 0x76  // v
        command[index++] = 0x30  // 0 (comando v 0)
        command[index++] = 0x00  // m = 0 (modo normal)
        
        // xL xH - ancho en bytes (little-endian)
        command[index++] = (widthBytes and 0xFF).toByte()
        command[index++] = ((widthBytes shr 8) and 0xFF).toByte()
        
        // yL yH - altura en pixels (little-endian)
        command[index++] = (height and 0xFF).toByte()
        command[index++] = ((height shr 8) and 0xFF).toByte()
        
        // Copiar datos de la imagen
        System.arraycopy(imageData, 0, command, index, imageData.size)
        
        return command
    }

    /**
     * Obtiene solo los datos rasterizados sin cabecera (para depuración)
     */
    fun getRasterDataOnly(context: Context, drawableId: Int): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
            val processed = processBitmap(bitmap,1)
            getRasterData(processed)
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf()
        }
    }
}
