package com.example.medicalreminder.ui.link

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Dibuja un código QR (ZXing calcula los módulos; aquí solo se pintan). Oscuro sobre blanco para que se lea bien. */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    dark: Color = Color(0xFF0B1B3F),
    light: Color = Color.White
) {
    val matrix = remember(content) {
        QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            mapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 0,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
        )
    }
    Canvas(modifier.aspectRatio(1f).background(light)) {
        val n = matrix.width
        val cell = size.width / n
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (matrix.get(x, y)) {
                    drawRect(dark, Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}

/**
 * Abre el escáner de QR de Google (no necesita permiso de cámara). Devuelve el texto decodificado.
 * Si el usuario lo cierra sin escanear, no pasa nada.
 */
fun scanQrCode(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode ->
            val raw = barcode.rawValue
            if (raw != null) onResult(raw) else onError("No se pudo leer el código.")
        }
        .addOnFailureListener {
            onError("No se pudo abrir el escáner. Revisa tu conexión y que el teléfono tenga Google Play, o escribe el código a mano.")
        }
}