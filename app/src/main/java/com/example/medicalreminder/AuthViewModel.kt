package com.example.medicalreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.SessionManager
import com.example.medicalreminder.network.ApiClient
import com.example.medicalreminder.network.ApiException
import kotlinx.coroutines.launch
import java.io.IOException

/** Inicio y cierre de sesión. La sesión (token + usuario) vive en SessionManager. */
class AuthViewModel(app: Application) : AndroidViewModel(app) {
    val session = SessionManager.get(app)

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val result = ApiClient.login(email, password)
                session.save(result.token, result.user)
                onSuccess()
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

    /**
     * Comprueba con el servidor que el token guardado siga siendo válido.
     * Si el servidor lo rechaza (401) cierra la sesión y avisa con onExpired.
     * Si no hay internet no hace nada: el usuario sigue dentro con los datos guardados.
     */
    fun refreshSession(onExpired: () -> Unit) {
        val token = session.token ?: return
        viewModelScope.launch {
            try {
                session.updateUser(ApiClient.me(token))
            } catch (e: ApiException) {
                if (e.code == 401) {
                    session.clear()
                    onExpired()
                }
            } catch (_: Exception) {
                // sin conexión u otro fallo: se conserva la sesión local
            }
        }
    }

    fun logout() = session.clear()
}