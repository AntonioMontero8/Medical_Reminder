package com.example.medicalreminder.data

import com.example.medicalreminder.ui.auth.UserRole

/** Datos del paso 1 (registro). Se conservan en memoria hasta completar el perfil (paso 2). */
data class PendingRegistration(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val role: UserRole
)

/** Equivale a la tabla `paciente`. Horarios en formato "HH:mm" (24 h) o null. */
data class PatientProfile(
    val edad: Int,
    val sexo: String,
    val peso: Float,
    val estatura: Int,
    val grupoSanguineo: String,
    val alergias: String,
    val padecimientos: String,
    val cirugias: String,
    val enfermedadesCronicas: String,
    val horarioDesayuno: String?,
    val horarioComida: String?,
    val horarioCena: String?
)

/** Equivale a la tabla `cuidador`. */
data class CaregiverProfile(
    val expProfesional: String
)