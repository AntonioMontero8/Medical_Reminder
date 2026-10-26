package com.example.medicalreminder.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Guarda el token y los datos básicos del usuario en el almacenamiento privado de la app,
 * para mantener la sesión iniciada aunque se cierre la aplicación.
 */
class SessionManager private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("medical_reminder_session", Context.MODE_PRIVATE)

    /** Usuario actual (null = sin sesión). Es estado de Compose: la interfaz se actualiza sola. */
    var user by mutableStateOf(readUser())
        private set

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val isLoggedIn: Boolean get() = token != null && user != null

    fun save(token: String, user: UserSession) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        writeUser(user)
    }

    /** Actualiza los datos del usuario (por ejemplo, tras validar con /api/me) sin tocar el token. */
    fun updateUser(user: UserSession) = writeUser(user)

    /** Cierra la sesión: borra el token y los datos guardados. */
    fun clear() {
        prefs.edit().clear().apply()
        user = null
    }

    private fun writeUser(u: UserSession) {
        prefs.edit()
            .putInt(KEY_ID, u.idUsuario)
            .putString(KEY_NAME, u.nombre)
            .putString(KEY_LAST, u.apellido)
            .putInt(KEY_TYPE, u.tipoUsuario)
            .apply()
        user = u
    }

    private fun readUser(): UserSession? {
        val id = prefs.getInt(KEY_ID, -1)
        if (id == -1 || prefs.getString(KEY_TOKEN, null) == null) return null
        return UserSession(
            idUsuario = id,
            nombre = prefs.getString(KEY_NAME, "").orEmpty(),
            apellido = prefs.getString(KEY_LAST, "").orEmpty(),
            tipoUsuario = prefs.getInt(KEY_TYPE, 1)
        )
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_ID = "id_usuario"
        private const val KEY_NAME = "nombre"
        private const val KEY_LAST = "apellido"
        private const val KEY_TYPE = "tipo_usuario"

        @Volatile private var instance: SessionManager? = null

        fun get(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context).also { instance = it }
            }
    }
}