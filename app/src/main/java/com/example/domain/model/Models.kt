package com.example.domain.model

enum class AttendanceStatus(val label: String, val code: String) {
    ALFA("Alfa", "A"),
    BERANGKAT("Berangkat", "B"),
    SAKIT("Sakit", "S"),
    IZIN("Izin", "I");

    companion object {
        fun fromString(value: String): AttendanceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: ALFA
        }
    }
}

enum class UserRole(val displayName: String) {
    ADMIN("Administrator"),
    KETUA_KELAS("Ketua Kelas"),
    WAKIL_KETUA_KELAS("Wakil Ketua Kelas");

    companion object {
        fun fromString(value: String): UserRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ADMIN
        }
    }
}

data class UserSession(
    val role: UserRole,
    val username: String,
    val displayName: String
)

data class AttendanceSummary(
    val total: Int,
    val berangkat: Int,
    val sakit: Int,
    val izin: Int,
    val alfa: Int
)
