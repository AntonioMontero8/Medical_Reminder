package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.ChatMessage
import com.example.medicalreminder.data.Conversation
import com.example.medicalreminder.data.Medication
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.data.TeamInfo
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Lado cuidador: equipo de cuidadores de un paciente, botiquín del paciente (solo lectura)
 * y mensajería directa entre cuidadores que comparten un paciente.
 */
class CareTeamViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager.get(app)

    // ---- Equipo del paciente (pantalla Tablero) ----
    var team by mutableStateOf<TeamInfo?>(null)
        private set
    private var teamFor = -1

    // ---- Botiquín del paciente ----
    var patientMeds by mutableStateOf<List<Medication>>(emptyList())
        private set
    var patientMedsLoaded by mutableStateOf(false)
        private set
    var patientMedsDenied by mutableStateOf(false) // el paciente no compartió su inventario
        private set
    private var medsFor = -1

    // ---- Mensajes ----
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var conversationsLoaded by mutableStateOf(false)
        private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var chatLoaded by mutableStateOf(false)
        private set
    var chatPartner by mutableStateOf(-1)
        private set
    var chatName by mutableStateOf("")
        private set
    var sending by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)

    val unreadTotal: Int get() = conversations.sumOf { it.noLeidos }

    private fun message(e: Exception) = when (e) {
        is ApiException -> e.message
        is IOException -> "No se pudo conectar con el servidor. Verifica que esté encendido."
        else -> "Ocurrió un error inesperado"
    }

    /** Las consultas "silent" (de refresco automático) no muestran errores: sin red simplemente se reintenta. */
    private fun launchApi(silent: Boolean = false, block: suspend (token: String) -> Unit) {
        val token = session.token ?: return
        viewModelScope.launch {
            try {
                block(token)
            } catch (e: Exception) {
                if (!silent) error = message(e)
            }
        }
    }

    // ------------------------- Equipo -------------------------

    fun clearTeam() { team = null; teamFor = -1 }

    fun loadTeam(linkId: Int, silent: Boolean = false) {
        teamFor = linkId
        launchApi(silent) { t ->
            val result = ApiClient.teamOf(t, linkId)
            if (teamFor == linkId) team = result
        }
    }

    // ------------------------- Botiquín del paciente -------------------------

    fun clearPatientMeds() {
        patientMeds = emptyList(); patientMedsLoaded = false; patientMedsDenied = false; medsFor = -1
    }

    fun loadPatientMeds(linkId: Int, silent: Boolean = false) {
        medsFor = linkId
        val token = session.token ?: return
        viewModelScope.launch {
            try {
                val list = ApiClient.patientMedications(token, linkId)
                if (medsFor == linkId) { patientMeds = list; patientMedsDenied = false; patientMedsLoaded = true }
            } catch (e: ApiException) {
                if (medsFor != linkId) return@launch
                if (e.code == 403) { patientMeds = emptyList(); patientMedsDenied = true; patientMedsLoaded = true }
                else if (!silent) { error = e.message; patientMedsLoaded = true }
            } catch (e: Exception) {
                if (!silent) { error = message(e); patientMedsLoaded = true }
            }
        }
    }

    // ------------------------- Mensajes -------------------------

    fun refreshConversations(silent: Boolean = false) = launchApi(silent) { t ->
        conversations = ApiClient.conversations(t)
        conversationsLoaded = true
    }

    /** Se llama justo antes de abrir un chat: deja la pantalla limpia para ese compañero. */
    fun openChat(otherId: Int, name: String) {
        chatPartner = otherId
        chatName = name
        messages = emptyList()
        chatLoaded = false
    }

    fun leaveChat(otherId: Int) {
        if (chatPartner == otherId) { chatPartner = -1; messages = emptyList(); chatLoaded = false }
    }

    /** Primera carga: los últimos mensajes. Con silent = true solo trae y agrega los nuevos. */
    fun loadChat(otherId: Int, silent: Boolean = false) = launchApi(silent) { t ->
        val after = if (silent) messages.lastOrNull()?.id else null
        val fresh = ApiClient.messagesWith(t, otherId, after)
        if (chatPartner != otherId) return@launchApi // ya se abrió otro chat
        if (after == null) {
            messages = fresh
        } else if (fresh.isNotEmpty()) {
            val known = messages.map { it.id }.toSet()
            messages = messages + fresh.filter { it.id !in known }
        }
        chatLoaded = true
    }

    fun send(otherId: Int, text: String, onSent: () -> Unit) {
        val token = session.token ?: return
        if (sending) return
        sending = true
        viewModelScope.launch {
            try {
                ApiClient.sendMessage(token, otherId, text)
                onSent()
                loadChat(otherId, silent = true)
            } catch (e: Exception) {
                error = message(e)
            } finally {
                sending = false
            }
        }
    }

    /** Al cerrar sesión: no deja datos de este usuario en memoria. */
    fun reset() {
        team = null; teamFor = -1
        clearPatientMeds()
        conversations = emptyList(); conversationsLoaded = false
        messages = emptyList(); chatLoaded = false; chatPartner = -1; chatName = ""
        sending = false
        error = null
    }
}