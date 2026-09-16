package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lapor_absen_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_KETUA_NAME = "ketua_name"
        private const val KEY_KETUA_PIN = "ketua_pin"
        private const val KEY_WAKIL_NAME = "wakil_name"
        private const val KEY_WAKIL_PIN = "wakil_pin"

        private const val KEY_CLASS_NAME = "class_name"
        private const val KEY_WALI_NAME = "wali_name"
        private const val KEY_WALI_PHONE = "wali_phone"

        private const val KEY_LOGGED_IN_ROLE = "logged_in_role"
        private const val KEY_LOGGED_IN_NAME = "logged_in_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        private const val KEY_REPORTER_ROLE = "reporter_role"
        private const val KEY_REPORTER_NAME = "reporter_name"
    }

    // Default initial values
    var reporterRole: UserRole
        get() {
            val roleStr = prefs.getString(KEY_REPORTER_ROLE, UserRole.KETUA_KELAS.name) ?: UserRole.KETUA_KELAS.name
            return UserRole.fromString(roleStr)
        }
        set(value) = prefs.edit().putString(KEY_REPORTER_ROLE, value.name).apply()

    var reporterName: String
        get() = prefs.getString(KEY_REPORTER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REPORTER_NAME, value.trim()).apply()

    var adminPassword: String
        get() = prefs.getString(KEY_ADMIN_PASSWORD, "admin") ?: "admin"
        set(value) = prefs.edit().putString(KEY_ADMIN_PASSWORD, value.trim()).apply()

    var ketuaName: String
        get() = prefs.getString(KEY_KETUA_NAME, "Ketua Kelas") ?: "Ketua Kelas"
        set(value) = prefs.edit().putString(KEY_KETUA_NAME, value.trim()).apply()

    var ketuaPin: String
        get() = prefs.getString(KEY_KETUA_PIN, "1111") ?: "1111"
        set(value) = prefs.edit().putString(KEY_KETUA_PIN, value.trim()).apply()

    var wakilName: String
        get() = prefs.getString(KEY_WAKIL_NAME, "Wakil Ketua Kelas") ?: "Wakil Ketua Kelas"
        set(value) = prefs.edit().putString(KEY_WAKIL_NAME, value.trim()).apply()

    var wakilPin: String
        get() = prefs.getString(KEY_WAKIL_PIN, "2222") ?: "2222"
        set(value) = prefs.edit().putString(KEY_WAKIL_PIN, value.trim()).apply()

    var className: String
        get() = prefs.getString(KEY_CLASS_NAME, "Kelas XII") ?: "Kelas XII"
        set(value) = prefs.edit().putString(KEY_CLASS_NAME, value.trim()).apply()

    var waliKelasName: String
        get() = prefs.getString(KEY_WALI_NAME, "Wali Kelas") ?: "Wali Kelas"
        set(value) = prefs.edit().putString(KEY_WALI_NAME, value.trim()).apply()

    var waliKelasPhone: String
        get() = prefs.getString(KEY_WALI_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WALI_PHONE, value.trim()).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var currentRole: UserRole
        get() {
            val roleStr = prefs.getString(KEY_LOGGED_IN_ROLE, UserRole.ADMIN.name) ?: UserRole.ADMIN.name
            return UserRole.fromString(roleStr)
        }
        set(value) = prefs.edit().putString(KEY_LOGGED_IN_ROLE, value.name).apply()

    var currentUserName: String
        get() = prefs.getString(KEY_LOGGED_IN_NAME, "Administrator") ?: "Administrator"
        set(value) = prefs.edit().putString(KEY_LOGGED_IN_NAME, value).apply()

    fun login(role: UserRole, displayName: String) {
        isLoggedIn = true
        currentRole = role
        currentUserName = displayName
    }

    fun logout() {
        isLoggedIn = false
        prefs.edit().remove(KEY_IS_LOGGED_IN).apply()
    }

    fun getCurrentSession(): UserSession {
        val role = reporterRole
        val name = if (reporterName.isNotBlank()) reporterName else role.displayName
        return UserSession(
            role = role,
            username = if (role == UserRole.KETUA_KELAS) "ketua" else "wakil",
            displayName = name
        )
    }
}
