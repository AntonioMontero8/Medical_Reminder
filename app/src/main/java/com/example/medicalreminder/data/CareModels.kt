package com.example.medicalreminder.data

import java.util.Calendar
import java.util.Locale

/** Otro cuidador del mismo paciente. */
data class TeamMember(val idUsuario: Int, val nombre: String, val apellido: String, val maestro: Boolean) {
    val fullName: String get() = "$nombre $apellido".trim()
}

/** Equipo de cuidadores de un paciente, visto por uno de ellos. */
data class TeamInfo(val iAmMaster: Boolean, val others: List<TeamMember>)

data class SharedPatient(val nombre: String, val apellido: String, val maestro: Boolean) {
    val fullName: String get() = "$nombre $apellido".trim()
}

/** Un compañero cuidador (comparte al menos un paciente contigo) con su último mensaje. */
data class Conversation(
    val idUsuario: Int,
    val nombre: String,
    val apellido: String,
    val esMaestro: Boolean,
    val pacientes: List<SharedPatient>,
    val ultimoTexto: String?,
    val ultimoMs: Long?,
    val ultimoMio: Boolean,
    val noLeidos: Int
) {
    val fullName: String get() = "$nombre $apellido".trim()
}

data class ChatMessage(val id: Long, val mio: Boolean, val contenido: String, val enviadoMs: Long)

/** "3:45 p. m." si es de hoy; "08/11 3:45 p. m." si es de otro día. */
fun formatMessageTime(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    val hour = c.get(Calendar.HOUR_OF_DAY)
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val time = "%d:%02d %s".format(Locale.US, h12, c.get(Calendar.MINUTE), if (hour < 12) "a. m." else "p. m.")
    val today = now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
    return if (today) time else "%02d/%02d %s".format(Locale.US, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, time)
}