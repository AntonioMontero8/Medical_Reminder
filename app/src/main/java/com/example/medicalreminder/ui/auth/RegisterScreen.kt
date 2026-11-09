package com.example.medicalreminder.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    var termsAccepted by rememberSaveable { mutableStateOf(false) }

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
        if (listOf(firstNameErr, lastNameErr, emailErr, passErr, confirmErr).all { it == null } && termsAccepted)
            onRegister(firstName.trim(), lastName.trim(), email.trim(), password, role)
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onBack, Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = MRColors.Navy)
            }
        }

        // Título Principal
        Text(
            text = "Toma el control de tu salud y dale tranquilidad a tu familia",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MRColors.Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Selector de Rol
        RoleSelector(role, onSelect = { role = it })
        Spacer(Modifier.height(16.dp))

        // Tarjeta de Beneficios Dinámica
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MRColors.Navy, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = if (role == UserRole.PATIENT) "Beneficios para el paciente:" else "Beneficios para el cuidador:",
                    fontWeight = FontWeight.Bold,
                    color = MRColors.Navy,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val benefits = if (role == UserRole.PATIENT) {
                    listOf(
                        "Nunca olvides una dosis con las alarmas inteligentes",
                        "Genera un historial automático para tu médico",
                        "Respaldo seguro de tus recetas y medicamentos",
                        "Tus cuidadores pueden ver en tiempo real el progreso en tus tratamientos gracias a la sincronización en la nube."
                    )
                } else {
                    listOf(
                        "Monitoreo en tiempo real de las tomas de tu paciente/familiar",
                        "Alertas automáticas si tu paciente/familiar olvida una dosis",
                        "Gestión compartida de horarios, inventario de medicamentos, recetas y tratamientos."
                    )
                }

                benefits.forEach { benefit ->
                    Row(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text("• ", fontWeight = FontWeight.Bold, color = MRColors.Navy)
                        Text(text = benefit, fontSize = 14.sp, color = MRColors.Navy)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Tabs: Crear cuenta | Iniciar sesión
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Crear cuenta", fontWeight = FontWeight.Bold, color = MRColors.Navy)
                HorizontalDivider(modifier = Modifier.width(100.dp).padding(top = 4.dp), thickness = 2.dp, color = MRColors.Navy)
            }
            HorizontalDivider(modifier = Modifier.width(1.dp).height(20.dp), color = MRColors.Navy)
            Text("Iniciar sesión", color = MRColors.Navy.copy(alpha = 0.6f))
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = MRColors.Navy.copy(alpha = 0.3f))
        Spacer(Modifier.height(24.dp))

        // Botón Continuar con Google
        OutlinedButton(
            onClick = { /* Acción de Google */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Continuar con Google", color = MRColors.Navy)
        }

        Spacer(Modifier.height(24.dp))

        // Formulario de Registro (Campos originales)
        AuthTextField(firstName, { firstName = it.take(50); firstNameErr = null }, "Nombre", errorText = firstNameErr)
        Spacer(Modifier.height(16.dp))
        AuthTextField(lastName, { lastName = it.take(50); lastNameErr = null }, "Apellido", errorText = lastNameErr)
        Spacer(Modifier.height(16.dp))
        AuthTextField(email, { email = it.take(150); emailErr = null }, "Correo",
            keyboardType = KeyboardType.Email, errorText = emailErr)
        Spacer(Modifier.height(16.dp))
        PasswordField(password, { password = it; passErr = null }, "Contraseña", errorText = passErr)
        Spacer(Modifier.height(10.dp))
        StrengthBar(password)
        Spacer(Modifier.height(16.dp))
        PasswordField(confirm, { confirm = it; confirmErr = null }, "Confirmación de contraseña",
            imeAction = ImeAction.Done, errorText = confirmErr)

        Spacer(Modifier.height(16.dp))

        // Checkbox de Términos y Condiciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it }
            )
            Text("Acepto los términos y condiciones de uso", fontSize = 14.sp, color = MRColors.Navy)
        }

        Spacer(Modifier.height(24.dp))

        // Botón Dinámico de Creación
        val buttonText = if (role == UserRole.PATIENT) "Crear cuenta de paciente" else "Crear cuenta de cuidador"
        PrimaryButton(buttonText, onClick = { focus.clearFocus(); submit() }, loading = loading)
        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Modo Oscuro")
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            onRegister = { _, _, _, _, _ -> },
            onBack = {}
        )
    }
}