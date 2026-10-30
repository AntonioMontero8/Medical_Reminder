package com.example.medicalreminder.network

import com.example.medicalreminder.data.AuthResult
import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.LinkCode
import com.example.medicalreminder.data.LinkInfo
import com.example.medicalreminder.data.LinkPermissions
import com.example.medicalreminder.data.PanelData
import com.example.medicalreminder.data.PatientCard
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.data.UserSession
import com.example.medicalreminder.ui.auth.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.os.Build

class ApiException(val code: Int, override val message: String) : Exception(message)

object ApiClient {
    // Emulador: 10.0.2.2 apunta al localhost de tu PC.
    // Celular físico: "localhost" si usas `adb reverse tcp:3000 tcp:3000`,
    // o la IP de tu PC en el Wi-Fi (por ejemplo "192.168.1.50").
    private const val PC_IP_FOR_PHONE = "localhost"

    private val IS_EMULATOR: Boolean
        get() = Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("emulator") ||
                Build.MODEL.contains("sdk_gphone") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK") ||
                Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("emulator") ||
                Build.FINGERPRINT.contains("emu64")

    val BASE_URL: String
        get() = if (IS_EMULATOR) "http://10.0.2.2:3000" else "http://$PC_IP_FOR_PHONE:3000"

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

    /** Crea la cuenta. Devuelve el token y el usuario, o lanza ApiException / IOException. */
    suspend fun register(body: JSONObject): AuthResult =
        parseAuth(call("POST", "/api/register", body))

    /** Autentica contra /api/login. Devuelve el token y el usuario. */
    suspend fun login(email: String, contrasena: String): AuthResult =
        parseAuth(call("POST", "/api/login", JSONObject().apply {
            put("correo", email)
            put("contrasena", contrasena)
        }))

    /** Valida el token guardado y devuelve los datos actuales. Lanza ApiException(401) si ya no sirve. */
    suspend fun me(token: String): UserSession =
        parseUser(call("GET", "/api/me", null, token).getJSONObject("user"))

    // ------------------------- Vinculación paciente <-> cuidador -------------------------

    /** Paciente: genera el código que se muestra como QR. */
    suspend fun createLinkCode(token: String): LinkCode {
        val j = call("POST", "/api/vinculos/codigo", JSONObject(), token)
        // Se calcula el vencimiento con el reloj del teléfono a partir de los segundos que dice el servidor
        return LinkCode(j.getString("codigo"), System.currentTimeMillis() + j.getInt("segundos") * 1000L)
    }

    /** Cuidador: nombre del paciente dueño del código (para confirmar antes de enviar la solicitud). */
    suspend fun previewLinkCode(token: String, code: String): String {
        val p = call("GET", "/api/vinculos/codigo/$code", null, token).getJSONObject("paciente")
        return "${p.optString("nombre")} ${p.optString("apellido")}".trim()
    }

    /** Cuidador: envía la solicitud de vinculación. */
    suspend fun requestLink(token: String, code: String): Int =
        call("POST", "/api/vinculos/solicitar", JSONObject().put("codigo", code), token).getInt("id_vinculacion")

    /** Lista de vínculos del usuario (cuidadores si es paciente, pacientes si es cuidador). */
    suspend fun listLinks(token: String): List<LinkInfo> {
        val arr = call("GET", "/api/vinculos", null, token).getJSONArray("vinculos")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LinkInfo(
                idVinculacion = o.getInt("id_vinculacion"),
                estado = o.getString("estado"),
                nombre = o.optString("nombre"),
                apellido = o.optString("apellido"),
                permisos = parsePerms(o.optJSONObject("permisos"))
            )
        }
    }

    /** Paciente: acepta (con permisos) o rechaza una solicitud pendiente. */
    suspend fun respondLink(token: String, id: Int, accept: Boolean, perms: LinkPermissions?) {
        val body = JSONObject().put("aceptar", accept)
        if (perms != null) body.put("permisos", permsJson(perms))
        call("POST", "/api/vinculos/$id/responder", body, token)
    }

    /** Paciente: cambia los permisos de un cuidador vinculado. */
    suspend fun updatePermissions(token: String, id: Int, perms: LinkPermissions) {
        call("PUT", "/api/vinculos/$id/permisos", JSONObject().put("permisos", permsJson(perms)), token)
    }

    /** Termina la vinculación (o cancela una solicitud pendiente). */
    suspend fun unlink(token: String, id: Int) {
        call("DELETE", "/api/vinculos/$id", null, token)
    }

    /** Cuidador: panel espejo del paciente. */
    suspend fun panel(token: String, id: Int): PanelData {
        val j = call("GET", "/api/vinculos/$id/panel", null, token)
        val p = j.getJSONObject("paciente")
        val f = j.optJSONObject("ficha")
        return PanelData(
            nombre = p.optString("nombre"),
            apellido = p.optString("apellido"),
            edad = if (p.isNull("edad")) null else p.getInt("edad"),
            permisos = parsePerms(j.optJSONObject("permisos")),
            ficha = f?.let {
                PatientCard(
                    sexo = it.str("sexo"),
                    peso = if (it.isNull("peso")) null else it.getDouble("peso"),
                    estatura = if (it.isNull("estatura")) null else it.getInt("estatura"),
                    grupoSanguineo = it.str("grupo_sanguineo"),
                    alergias = it.str("alergias"),
                    padecimientos = it.str("padecimientos"),
                    cirugias = it.str("cirugias"),
                    enfermedadesCronicas = it.str("enfermedades_cronicas"),
                    desayuno = it.str("horario_desayuno"),
                    comida = it.str("horario_comida"),
                    cena = it.str("horario_cena")
                )
            }
        )
    }

    /** Envía la solicitud de recuperación de contraseña al servidor (/api/forgot-password) */
    suspend fun recoverPassword(email: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("correo", email)
        }
        val conn = (URL("$BASE_URL/api/forgot-password").openConnection() as HttpURLConnection).apply {
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

            if (code in 200..299) {
                true
            } else {
                throw ApiException(
                    code,
                    json?.optString("error").takeUnless { it.isNullOrBlank() } ?: "Error del servidor ($code)"
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    // org.json devuelve el texto "null" con optString cuando el valor es null: por eso este ayudante
    private fun JSONObject.str(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun parsePerms(o: JSONObject?) = LinkPermissions(
        ficha = o?.optBoolean("ficha", true) ?: true,
        inventario = o?.optBoolean("inventario", true) ?: true,
        historial = o?.optBoolean("historial", true) ?: true,
        alertas = o?.optBoolean("alertas", true) ?: true,
        gestionar = o?.optBoolean("gestionar", false) ?: false
    )

    private fun permsJson(p: LinkPermissions) = JSONObject()
        .put("ficha", p.ficha)
        .put("inventario", p.inventario)
        .put("historial", p.historial)
        .put("alertas", p.alertas)
        .put("gestionar", p.gestionar)

    private fun parseUser(u: JSONObject) = UserSession(
        idUsuario = u.getInt("id_usuario"),
        nombre = u.optString("nombre"),
        apellido = u.optString("apellido"),
        tipoUsuario = u.getInt("tipo_usuario")
    )

    private fun parseAuth(j: JSONObject) = AuthResult(j.getString("token"), parseUser(j.getJSONObject("user")))

    /** Una sola función para todas las peticiones. */
    private suspend fun call(method: String, path: String, body: JSONObject?, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000
                readTimeout = 8000
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (code in 200..299) json ?: JSONObject()
                else throw ApiException(
                    code,
                    json?.optString("error").takeUnless { it.isNullOrBlank() } ?: "Error del servidor ($code)"
                )
            } finally {
                conn.disconnect()
            }
        }
}