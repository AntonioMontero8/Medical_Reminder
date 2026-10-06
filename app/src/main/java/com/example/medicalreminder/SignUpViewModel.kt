package com.example.medicalreminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/** Guarda los datos del paso 1 y envía el registro completo al servidor al terminar el paso 2. */
class SignUpViewModel : ViewModel() {
    var pending by mutableStateOf<PendingRegistration?>(null)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

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
                ApiClient.register(body(p))
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