package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.LinkCode
import com.example.medicalreminder.data.LinkInfo
import com.example.medicalreminder.data.LinkPermissions
import com.example.medicalreminder.data.PanelData
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado compartido de todas las pantallas de vinculación (paciente y cuidador). */
class LinkViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager.get(app)

    var links by mutableStateOf<List<LinkInfo>>(emptyList())
        private set
    var linksLoaded by mutableStateOf(false)
        private set
    var code by mutableStateOf<LinkCode?>(null)
        private set
    var panel by mutableStateOf<PanelData?>(null)
        private set
    var error by mutableStateOf<String?>(null)

    private var busyCount by mutableIntStateOf(0)
    /** true mientras hay una acción del usuario en curso (generar código, enviar, aceptar...). */
    val busy: Boolean get() = busyCount > 0

    private val handledPending = mutableSetOf<Int>()
    private var generating = false

    /**
     * Ejecuta una llamada a la API con el token guardado.
     * track = muestra "ocupado" en los botones; report = muestra el error al usuario
     * (el sondeo automático usa report = false para no llenar la pantalla de avisos).
     */
    private fun launchApi(track: Boolean = true, report: Boolean = true, block: suspend (token: String) -> Unit) {
        val token = session.token ?: return
        viewModelScope.launch {
            if (track) busyCount++
            try {
                block(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (report) error = e.message
            } catch (e: IOException) {
                if (report) error = "No se pudo conectar con el servidor. Verifica que esté encendido."
            } catch (e: Exception) {
                if (report) error = "Ocurrió un error inesperado"
            } finally {
                if (track) busyCount--
            }
        }
    }

    fun refreshLinks(silent: Boolean = false) = launchApi(track = false, report = !silent) { t ->
        links = ApiClient.listLinks(t)
        linksLoaded = true
    }

    // ---- Paciente: QR ----
    fun generateCode() {
        if (generating || session.token == null) return
        generating = true
        launchApi { t ->
            try {
                code = ApiClient.createLinkCode(t)
            } finally {
                generating = false
            }
        }
    }

    fun clearCode() { code = null }

    /** true solo la primera vez que se ve una solicitud pendiente (para abrirla una única vez). */
    fun markPendingHandled(id: Int): Boolean = handledPending.add(id)

    // ---- Cuidador: escaneo y solicitud ----
    fun previewCode(code: String, onResult: (String) -> Unit) = launchApi { t ->
        onResult(ApiClient.previewLinkCode(t, code))
    }

    fun requestLink(code: String, onDone: () -> Unit) = launchApi { t ->
        ApiClient.requestLink(t, code)
        onDone()
    }

    // ---- Paciente: confirmar y administrar permisos ----
    fun respond(id: Int, accept: Boolean, perms: LinkPermissions?, onDone: () -> Unit) = launchApi { t ->
        ApiClient.respondLink(t, id, accept, perms)
        links = ApiClient.listLinks(t)
        onDone()
    }

    fun savePermissions(id: Int, perms: LinkPermissions, onDone: () -> Unit) = launchApi { t ->
        ApiClient.updatePermissions(t, id, perms)
        links = ApiClient.listLinks(t)
        onDone()
    }

    fun unlink(id: Int, onDone: () -> Unit) = launchApi { t ->
        ApiClient.unlink(t, id)
        links = ApiClient.listLinks(t)
        onDone()
    }

    // ---- Cuidador: panel espejo ----
    fun loadPanel(id: Int, silent: Boolean = false) = launchApi(track = false, report = !silent) { t ->
        panel = ApiClient.panel(t, id)
    }

    fun clearPanel() { panel = null }

    /** Se llama al cerrar sesión para que el siguiente usuario no vea datos del anterior. */
    fun reset() {
        links = emptyList()
        linksLoaded = false
        code = null
        panel = null
        error = null
        handledPending.clear()
        generating = false
    }
}