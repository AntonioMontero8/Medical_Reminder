package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.data.ProfileData
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.data.UserSession
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/** Carga y edición del perfil del usuario con sesión. */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager.get(app)

    var profile by mutableStateOf<ProfileData?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    private fun message(e: Exception) = when (e) {
        is ApiException -> e.message
        is IOException -> "No se pudo conectar con el servidor. Verifica que esté encendido."
        else -> "Ocurrió un error inesperado"
    }

    /** Trae el perfil actual desde el servidor. Se llama al abrir la pantalla de edición. */
    fun load() {
        val token = session.token ?: return
        profile = null
        loading = true
        viewModelScope.launch {
            try {
                profile = ApiClient.getProfile(token)
            } catch (e: Exception) {
                error = message(e)
            } finally {
                loading = false
            }
        }
    }

    fun savePatient(nombre: String, apellido: String, data: PatientProfile, onDone: () -> Unit) =
        save(onDone) { token -> ApiClient.updatePatientProfile(token, nombre, apellido, data) }

    fun saveCaregiver(nombre: String, apellido: String, data: CaregiverProfile, onDone: () -> Unit) =
        save(onDone) { token -> ApiClient.updateCaregiverProfile(token, nombre, apellido, data) }

    private fun save(onDone: () -> Unit, request: suspend (String) -> UserSession) {
        val token = session.token ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                // El nombre nuevo se refleja al instante en el saludo de la pantalla principal
                session.updateUser(request(token))
                onDone()
            } catch (e: Exception) {
                error = message(e)
            } finally {
                saving = false
            }
        }
    }

    /** Al cerrar sesión: no deja datos de este usuario en memoria. */
    fun reset() {
        profile = null
        loading = false
        saving = false
        error = null
    }
}