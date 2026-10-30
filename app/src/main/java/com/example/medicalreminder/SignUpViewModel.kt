package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/** Guarda los datos del paso 1 y envía el registro completo al servidor al terminar el paso 2. */
class SignUpViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager.get(app)

    var pending by mutableStateOf<PendingRegistration?>(null)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    var successMessage by mutableStateOf<String?>(null)

    fun recoverPassword(email: String, onSuccess: () -> Unit) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                ApiClient.recoverPassword(email)
                successMessage = "Se han enviado las instrucciones a tu correo."
                onSuccess()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: java.io.IOException) {
                error = "No se pudo conectar con el servidor. Verifica que esté encendido."
            } catch (e: Exception) {
                error = "Ocurrió un error inesperado"
            } finally {
                loading = false
            }
        }
    }

    fun submitPatient(profile: PatientProfile, onSuccess: () -> Unit) =
        submit(onSuccess) { ApiClient.patientBody(it, profile) }

    fun submitCaregiver(profile: CaregiverProfile, onSuccess: () -> Unit) =
        submit(onSuccess) { ApiClient.caregiverBody(it, profile) }

    private fun submit(onSuccess: () -> Unit, body: (PendingRegistration) -> org.json.JSONObject) {
        val p = pending ?: return
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                val result = ApiClient.register(body(p))
                // Al crear la cuenta se inicia sesión automáticamente
                session.save(result.token, result.user)
                onSuccess()
                pending = null
            } catch (e: ApiException) {
                error = e.message
            } catch (e: IOException) {
                error = "No se pudo conectar con el servidor. Verifica que esté encendido."
            } catch (e: Exception) {
                error = "Ocurrió un error inesperado"
            } finally {
                loading = false
            }
        }
    }
}