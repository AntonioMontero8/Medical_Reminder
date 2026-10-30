package com.example.medicalreminder.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.ui.theme.MRColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onSendResetLink: (email: String) -> Unit,
    onBack: () -> Unit,
    loading: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    fun submit() {
        emailError = if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) null else "Ingresa un correo válido"
        if (emailError == null) {
            onSendResetLink(email.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Recuperar contraseña",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MRColors.Navy,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Ingresa el correo electrónico asociado a tu cuenta para enviarte las instrucciones de recuperación.",
                fontSize = 16.sp,
                color = MRColors.TextGray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            AuthTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "Correo Electrónico",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                errorText = emailError
            )

            Spacer(Modifier.height(32.dp))
            PrimaryButton(
                "ENVIAR INSTRUCCIONES",
                onClick = { focus.clearFocus(); submit() },
                loading = loading
            )
        }
    }
}