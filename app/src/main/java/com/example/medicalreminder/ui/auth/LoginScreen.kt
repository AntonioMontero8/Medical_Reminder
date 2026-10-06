package com.example.medicalreminder.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.ui.theme.MRColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onForgotPassword: () -> Unit,
    onGoToRegister: () -> Unit,
    loading: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passError by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    fun submit() {
        emailError = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) null else "Ingresa un correo válido"
        passError = if (password.isNotEmpty()) null else "Ingresa tu contraseña"
        if (emailError == null && passError == null) onLogin(email.trim(), password)
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        BrandHeader()
        Spacer(Modifier.height(32.dp))
        Text("Bienvenido", fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold,
            color = MRColors.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Ingresa para gestionar tus medicamentos", fontSize = 18.sp, color = MRColors.TextGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        AuthTextField(
            value = email, onValueChange = { email = it; emailError = null },
            placeholder = "Correo Electrónico", leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email, errorText = emailError
        )
        Spacer(Modifier.height(16.dp))
        PasswordField(
            value = password, onValueChange = { password = it; passError = null },
            placeholder = "Contraseña", withLockIcon = true,
            imeAction = ImeAction.Done, errorText = passError
        )
        Text(
            "¿Olvidaste tu contraseña?",
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MRColors.Navy,
            modifier = Modifier.align(Alignment.End).clickable(onClick = onForgotPassword).padding(vertical = 12.dp)
        )

        Spacer(Modifier.height(48.dp))
        PrimaryButton("INICIAR SESIÓN", onClick = { focus.clearFocus(); submit() }, loading = loading)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.clickable(onClick = onGoToRegister).padding(8.dp)) {
            Text("¿No tienes una cuenta? ", fontSize = 16.sp, color = MRColors.Navy)
            Text("Regístrate aquí", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Modo Oscuro")
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onLogin = { _, _ -> },
            onForgotPassword = {},
            onGoToRegister = {}
        )
    }
}