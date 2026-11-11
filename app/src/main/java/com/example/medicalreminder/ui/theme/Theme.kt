package com.example.medicalreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta única de la aplicación. No se usan colores fuera de estos diez.
 *
 * Criterio médico de uso:
 *  - Azul .............. acción y confianza (botones, selección, foco, enlaces, mensajes propios)
 *  - Verde ............. SOLO estados de salud/éxito (activo, vinculado, existencia suficiente, contraseña fuerte)
 *  - Rojo .............. peligro o algo irreversible (eliminar, desvincular, rechazar, errores, agotado, caducado)
 *  - Coral / Rosa ...... advertencia que aún se puede atender (pocas dosis, código por vencer)
 *  - Celestes .......... información neutra (pendiente, avatares, fondos suaves, campos)
 *  - Tinta / Pizarra ... Texto, contornos y texto secundario
 */
object MRColors {
    // ---------- Paleta base ----------
    val Green = Color(0xFF00B07A)
    val Blue = Color(0xFF005CB9)
    val SkyBlue = Color(0xFF89CFFF)
    val IceBlue = Color(0xFFDBEDFB)
    val Red = Color(0xFFE31F26)
    val Coral = Color(0xFFE56363)
    val Blush = Color(0xFFFCD3CF)
    val Ink = Color(0xFF1D2631)
    val Slate = Color(0xFF617288)
    val White = Color(0xFFFFFFFF)

    // ---------- Nombres anteriores (apuntan a la paleta para que todas las pantallas se recoloreen) ----------
    val Navy = Ink
    val Mint = Blue            // botón principal
    val DarkTeal = White       // texto sobre el botón principal
    val MintStrong = SkyBlue
    val MintSoft = IceBlue
    val Background = White
    val FieldBg = White        // campos de texto: blanco con contorno pizarra (mejor contraste para el texto de ayuda)
    val FieldBorder = Slate
    val TextGray = Slate
    val StrengthOn = Green
    val StrengthOff = IceBlue
    val Error = Red
    val Orange = Blush
    val ChipGray = IceBlue
}

private val Scheme = lightColorScheme(
    primary = MRColors.Blue,
    onPrimary = MRColors.White,
    primaryContainer = MRColors.SkyBlue,
    onPrimaryContainer = MRColors.Ink,
    secondary = MRColors.Green,
    onSecondary = MRColors.Ink,
    secondaryContainer = MRColors.IceBlue,
    onSecondaryContainer = MRColors.Ink,
    tertiary = MRColors.Coral,
    onTertiary = MRColors.Ink,
    tertiaryContainer = MRColors.Blush,
    onTertiaryContainer = MRColors.Ink,
    background = MRColors.White,
    onBackground = MRColors.Ink,
    surface = MRColors.White,
    onSurface = MRColors.Ink,
    surfaceVariant = MRColors.IceBlue,
    onSurfaceVariant = MRColors.Slate,
    surfaceTint = MRColors.White,
    inverseSurface = MRColors.Ink,
    inverseOnSurface = MRColors.White,
    error = MRColors.Red,
    onError = MRColors.White,
    errorContainer = MRColors.Blush,
    onErrorContainer = MRColors.Ink,
    outline = MRColors.Slate,
    outlineVariant = MRColors.SkyBlue,
    surfaceContainerLowest = MRColors.White,
    surfaceContainerLow = MRColors.White,
    surfaceContainer = MRColors.White,
    surfaceContainerHigh = MRColors.White,
    surfaceContainerHighest = MRColors.IceBlue
)

@Composable
fun MedicalReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}