package com.example.medicalreminder.network

import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.ui.auth.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val code: Int, override val message: String) : Exception(message)

object ApiClient {
    // Emulador de Android Studio: 10.0.2.2 apunta al localhost de tu PC.
    // Celular físico: usa la IP local de tu PC, por ejemplo "http://192.168.1.50:3000".
    const val BASE_URL = "http://10.0.2.2:3000"

    private fun tipoUsuario(role: UserRole) = if (role == UserRole.PATIENT) 1 else 2

    private fun baseBody(p: PendingRegistration) = JSONObject().apply {
        put("nombre", p.firstName)
        put("apellido", p.lastName)
        put("correo", p.email)
        put("contrasena", p.password)
        put("tipoUsuario", tipoUsuario(p.role))
    }

    fun patientBody(p: PendingRegistration, x: PatientProfile): JSONObject = baseBody(p).apply {
        put("perfil", JSONObject().apply {
            put("edad", x.edad)
            put("sexo", x.sexo)
            put("peso", x.peso.toString().toDouble()) // evita 70.0999984 al convertir Float -> Double
            put("estatura", x.estatura)
            put("grupoSanguineo", x.grupoSanguineo)
            put("alergias", x.alergias)
            put("padecimientos", x.padecimientos)
            put("cirugias", x.cirugias)
            put("enfermedadesCronicas", x.enfermedadesCronicas)
            put("horarioDesayuno", x.horarioDesayuno ?: JSONObject.NULL)
            put("horarioComida", x.horarioComida ?: JSONObject.NULL)
            put("horarioCena", x.horarioCena ?: JSONObject.NULL)
        })
    }

    fun caregiverBody(p: PendingRegistration, x: CaregiverProfile): JSONObject = baseBody(p).apply {
        put("perfil", JSONObject().apply {
            put("expProfesional", x.expProfesional)
        })
    }

    /** Devuelve el id_usuario creado o lanza ApiException / IOException. */
    suspend fun register(body: JSONObject): Int = withContext(Dispatchers.IO) {
        val conn = (URL("$BASE_URL/api/register").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (code in 200..299) json?.optInt("id_usuario", -1) ?: -1
            else throw ApiException(code, json?.optString("error").takeUnless { it.isNullOrBlank() } ?: "Error del servidor ($code)")
        } finally {
            conn.disconnect()
        }
    }
}