package com.example.medicalreminder.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.ui.theme.MRColors
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RegisterScreen(
    onRegister: (firstName: String, lastName: String, email: String, password: String, role: UserRole) -> Unit,
    onBack: () -> Unit,
    loading: Boolean = false
) {
    var role by rememberSaveable { mutableStateOf(UserRole.PATIENT) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var firstNameErr by remember { mutableStateOf<String?>(null) }
    var lastNameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var passErr by remember { mutableStateOf<String?>(null) }
    var confirmErr by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    fun submit() {
        firstNameErr = if (firstName.isNotBlank()) null else "Ingresa tu nombre o nombres"
        lastNameErr = if (lastName.isNotBlank()) null else "Ingresa tus apellidos"
        emailErr = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) null else "Ingresa un correo válido"
        passErr = if (passwordStrength(password) >= 3 && password.length >= 8) null else "Mínimo 8 caracteres, con mayúsculas, números o símbolos"
        confirmErr = if (password == confirm) null else "Las contraseñas no coinciden"
        if (listOf(firstNameErr, lastNameErr, emailErr, passErr, confirmErr).all { it == null })
            onRegister(firstName.trim(), lastName.trim(), email.trim(), password, role)
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack, Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = MRColors.Navy)
        }
        Text("Crear Cuenta", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Navy,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 20.dp))

        RoleSelector(role, onSelect = { role = it })
        Spacer(Modifier.height(28.dp))

        // La tabla usuario guarda nombre_usuario y apellido_usuario por separado (máx. 50 caracteres cada uno)
        AuthTextField(firstName, { firstName = it.take(50); firstNameErr = null }, "Nombre(s)", errorText = firstNameErr)
        Spacer(Modifier.height(16.dp))
        AuthTextField(lastName, { lastName = it.take(50); lastNameErr = null }, "Apellidos", errorText = lastNameErr)
        Spacer(Modifier.height(16.dp))
        AuthTextField(email, { email = it.take(150); emailErr = null }, "Correo Electrónico",
            keyboardType = KeyboardType.Email, errorText = emailErr)
        Spacer(Modifier.height(16.dp))
        PasswordField(password, { password = it; passErr = null }, "Contraseña", errorText = passErr)
        Spacer(Modifier.height(10.dp))
        StrengthBar(password)
        Spacer(Modifier.height(16.dp))
        PasswordField(confirm, { confirm = it; confirmErr = null }, "Confirmar Contraseña",
            imeAction = ImeAction.Done, errorText = confirmErr)

        Spacer(Modifier.height(32.dp))
        PrimaryButton("CREAR CUENTA", onClick = { focus.clearFocus(); submit() }, loading = loading)
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Modo Oscuro")
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            onRegister = {_, _, _, _, _, ->},
            onBack = {}
        )
    }
}