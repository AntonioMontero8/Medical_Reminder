package com.example.medicalreminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalreminder.data.LoginRequest
import com.example.medicalreminder.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            error = null

            val result = ApiClient.login(LoginRequest(email, password))

            withContext(Dispatchers.Main) {
                loading = false
                if (result.isSuccess) {
                    // Opcional: Guardar token de sesión en EncryptedSharedPreferences / DataStore
                    onSuccess()
                } else {
                    error = result.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                }
            }
        }
    }
}