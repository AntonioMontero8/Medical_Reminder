package com.example.medicalreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MRColors {
    val Navy = Color(0xFF0B1B3F)
    val Mint = Color(0xFF9AEFD3)
    val MintStrong = Color(0xFF8FEBCB)
    val MintSoft = Color(0xFFE3FBF2)
    val DarkTeal = Color(0xFF0B3B3F)
    val Background = Color(0xFFFCF9FF)
    val FieldBg = Color(0xFFF0F2F6)
    val FieldBorder = Color(0xFF9AA5B8)
    val TextGray = Color(0xFF5F6673)
    val StrengthOn = Color(0xFF6FAF4F)
    val StrengthOff = Color(0xFFE0E3E8)
    val Error = Color(0xFFB3261E)
    val Orange = Color(0xFFFF9B71)
    val ChipGray = Color(0xFFE9ECF1)
}

private val Scheme = lightColorScheme(
    primary = MRColors.Mint,
    onPrimary = MRColors.DarkTeal,
    background = MRColors.Background,
    surface = MRColors.Background,
    onBackground = MRColors.Navy,
    onSurface = MRColors.Navy,
    error = MRColors.Error
)

@Composable
fun MedicalReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}