package com.example.medicalreminder.data

import com.example.medicalreminder.ui.auth.UserRole

/** Datos básicos del usuario con sesión iniciada. tipoUsuario: 1 = paciente, 2 = cuidador. */
data class UserSession(
    val idUsuario: Int,
    val nombre: String,
    val apellido: String,
    val tipoUsuario: Int
) {
    val role: UserRole get() = if (tipoUsuario == 1) UserRole.PATIENT else UserRole.CAREGIVER
}

/** Respuesta del servidor en login y registro. */
data class AuthResult(
    val token: String,
    val user: UserSession
)