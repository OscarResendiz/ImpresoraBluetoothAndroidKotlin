package com.example.impresora

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

/**
 * Generador de códigos QR usando ZXing
 */
object QRCodeGenerator {

    /**
     * Genera un código QR a partir de un texto/UUID
     * @param text El contenido del QR (ej: UUID)
     * @param size El tamaño en pixels del QR (preferiblemente múltiplo de 10)
     * @return Bitmap con el código QR, o null si hay error
     */
    fun generateQRCode(text: String, size: Int = 300): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            
            createBitmapFromBitMatrix(bitMatrix)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convierte BitMatrix a Bitmap
     */
    private fun createBitmapFromBitMatrix(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        
        return bitmap
    }
}
