package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.Medication
import com.example.medicalreminder.data.MedicationInput
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/** Botiquín del paciente: lista de medicamentos y registro, edición y borrado. */
class MedicationViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager.get(app)

    var medications by mutableStateOf<List<Medication>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    private fun message(e: Exception) = when (e) {
        is ApiException -> e.message
        is IOException -> "No se pudo conectar con el servidor. Verifica que esté encendido."
        else -> "Ocurrió un error inesperado"
    }

    fun load() {
        val token = session.token ?: return
        viewModelScope.launch {
            try {
                medications = ApiClient.listMedications(token)
                loaded = true
            } catch (e: Exception) {
                error = message(e)
                loaded = true
            }
        }
    }

    fun save(input: MedicationInput, onDone: () -> Unit) =
        mutate(onDone) { token -> ApiClient.createMedication(token, input) }

    fun update(id: Int, input: MedicationInput, onDone: () -> Unit) =
        mutate(onDone) { token -> ApiClient.updateMedication(token, id, input) }

    fun delete(id: Int, onDone: () -> Unit) =
        mutate(onDone) { token -> ApiClient.deleteMedication(token, id) }

    /** Ejecuta un cambio y, si sale bien, refresca la lista y avisa con onDone. */
    private fun mutate(onDone: () -> Unit, request: suspend (String) -> Unit) {
        val token = session.token ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                request(token)
                runCatching { medications = ApiClient.listMedications(token); loaded = true }
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
        medications = emptyList()
        loaded = false
        saving = false
        error = null
    }
}