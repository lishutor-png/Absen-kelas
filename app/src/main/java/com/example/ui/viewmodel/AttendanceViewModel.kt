package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AttendanceRepository
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceSummary
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.utils.BackupRestoreUtils
import com.example.utils.DateTimeUtils
import com.example.utils.WhatsAppUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.studentDao(), database.attendanceDao())

    // Session State
    private val _currentSession = MutableStateFlow<UserSession?>(userPrefs.getCurrentSession())
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    // Selected Date for Attendance (YYYY-MM-DD)
    private val _selectedDate = MutableStateFlow(DateTimeUtils.getTodayDbDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // All Students list from Database
    val students: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance Records for current selected date
    @OptIn(ExperimentalCoroutinesApi::class)
    val recordsForSelectedDate: StateFlow<List<AttendanceRecordEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getRecordsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time summary counts
    val attendanceSummary: StateFlow<AttendanceSummary> = combine(
        students,
        recordsForSelectedDate
    ) { studentList, records ->
        val total = studentList.size
        var berangkat = 0
        var sakit = 0
        var izin = 0
        var alfa = 0

        // If record exists, count its status; if missing, it counts as Alfa (default)
        val recordMap = records.associateBy { it.studentId }
        studentList.forEach { student ->
            val status = recordMap[student.id]?.status ?: AttendanceStatus.ALFA.name
            when (status) {
                AttendanceStatus.BERANGKAT.name -> berangkat++
                AttendanceStatus.SAKIT.name -> sakit++
                AttendanceStatus.IZIN.name -> izin++
                else -> alfa++
            }
        }

        AttendanceSummary(
            total = total,
            berangkat = berangkat,
            sakit = sakit,
            izin = izin,
            alfa = alfa
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AttendanceSummary(0, 0, 0, 0, 0)
    )

    // Reporter State (Role & Name as requested by user)
    private val _reporterRole = MutableStateFlow(userPrefs.reporterRole)
    val reporterRole: StateFlow<UserRole> = _reporterRole.asStateFlow()

    private val _reporterName = MutableStateFlow(userPrefs.reporterName)
    val reporterName: StateFlow<String> = _reporterName.asStateFlow()

    // Settings State
    private val _className = MutableStateFlow(userPrefs.className)
    val className: StateFlow<String> = _className.asStateFlow()

    private val _waliKelasName = MutableStateFlow(userPrefs.waliKelasName)
    val waliKelasName: StateFlow<String> = _waliKelasName.asStateFlow()

    private val _waliKelasPhone = MutableStateFlow(userPrefs.waliKelasPhone)
    val waliKelasPhone: StateFlow<String> = _waliKelasPhone.asStateFlow()

    private val _ketuaName = MutableStateFlow(userPrefs.ketuaName)
    val ketuaName: StateFlow<String> = _ketuaName.asStateFlow()

    private val _wakilName = MutableStateFlow(userPrefs.wakilName)
    val wakilName: StateFlow<String> = _wakilName.asStateFlow()

    // Status / Feedback message (for snackbars)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        // Initial setup: ensure default records exist for today if students are present
        refreshRecordsForDate(_selectedDate.value)
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun updateReporterProfile(role: UserRole, name: String) {
        userPrefs.reporterRole = role
        userPrefs.reporterName = name
        _reporterRole.value = role
        _reporterName.value = name
        _currentSession.value = userPrefs.getCurrentSession()
        _feedbackMessage.value = "Identitas pelapor diatur: ${role.displayName} ${if (name.isNotBlank()) "($name)" else ""}"
    }

    // --- Authentication ---

    fun login(role: UserRole, credentialInput: String): Boolean {
        val isValid = when (role) {
            UserRole.ADMIN -> {
                credentialInput.trim() == userPrefs.adminPassword
            }
            UserRole.KETUA_KELAS -> {
                credentialInput.trim() == userPrefs.ketuaPin
            }
            UserRole.WAKIL_KETUA_KELAS -> {
                credentialInput.trim() == userPrefs.wakilPin
            }
        }

        if (isValid) {
            val displayName = when (role) {
                UserRole.ADMIN -> "Administrator"
                UserRole.KETUA_KELAS -> userPrefs.ketuaName
                UserRole.WAKIL_KETUA_KELAS -> userPrefs.wakilName
            }
            userPrefs.login(role, displayName)
            _currentSession.value = userPrefs.getCurrentSession()
            // Make sure records are prepared
            refreshRecordsForDate(_selectedDate.value)
            return true
        }
        return false
    }

    fun logout() {
        userPrefs.logout()
        _currentSession.value = null
    }

    // --- Attendance Operations ---

    fun setDate(newDate: String) {
        _selectedDate.value = newDate
        refreshRecordsForDate(newDate)
    }

    fun refreshRecordsForDate(date: String) {
        viewModelScope.launch {
            val reporter = _currentSession.value?.displayName ?: "Sistem"
            repository.ensureRecordsForDate(date, reporter)
        }
    }

    fun updateStudentStatus(studentId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val reporter = _currentSession.value?.displayName ?: "Pengurus Kelas"
            repository.updateStatus(_selectedDate.value, studentId, status, reporter)
        }
    }

    fun updateStudentNote(studentId: Long, note: String) {
        viewModelScope.launch {
            repository.updateRecordNote(_selectedDate.value, studentId, note)
        }
    }

    fun markAllBerangkat() {
        viewModelScope.launch {
            val reporter = _currentSession.value?.displayName ?: "Pengurus Kelas"
            repository.setAllStatus(_selectedDate.value, AttendanceStatus.BERANGKAT, reporter)
            _feedbackMessage.value = "Semua siswa ditandai Berangkat."
        }
    }

    fun resetAllToAlfa() {
        viewModelScope.launch {
            val reporter = _currentSession.value?.displayName ?: "Pengurus Kelas"
            repository.setAllStatus(_selectedDate.value, AttendanceStatus.ALFA, reporter)
            _feedbackMessage.value = "Absensi direset ke default (Alfa)."
        }
    }

    // --- Student Management ---

    fun importStudents(commaSeparatedText: String, append: Boolean = false) {
        viewModelScope.launch {
            val count = repository.importStudentsFromCommaSeparated(commaSeparatedText, append)
            if (count > 0) {
                _feedbackMessage.value = "Berhasil memproses $count siswa secara abjad."
                // Reinitialize today's records with ALFA default
                refreshRecordsForDate(_selectedDate.value)
            } else {
                _feedbackMessage.value = "Tidak ada nama siswa valid yang ditemukan."
            }
        }
    }

    fun addSingleStudent(name: String) {
        viewModelScope.launch {
            val success = repository.addSingleStudent(name)
            if (success) {
                _feedbackMessage.value = "Siswa '$name' berhasil ditambahkan."
                refreshRecordsForDate(_selectedDate.value)
            } else {
                _feedbackMessage.value = "Gagal menambah siswa (nama sudah ada atau kosong)."
            }
        }
    }

    fun deleteStudent(studentId: Long) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            _feedbackMessage.value = "Siswa berhasil dihapus & nomor absen diurutkan ulang."
            refreshRecordsForDate(_selectedDate.value)
        }
    }

    // --- WhatsApp Reporting ---

    fun sendWhatsAppReport(context: Context, absentOnly: Boolean) {
        val currentRecords = recordsForSelectedDate.value
        val summary = attendanceSummary.value
        val role = _reporterRole.value
        val reporterRole = role.displayName
        val reporterName = _reporterName.value.ifBlank { role.displayName }
        val timeNow = DateTimeUtils.getCurrentTime()

        val textReport = if (absentOnly) {
            WhatsAppUtils.generateAbsentOnlyReport(
                className = _className.value,
                waliKelasName = _waliKelasName.value,
                reporterRole = reporterRole,
                reporterName = reporterName,
                dbDate = _selectedDate.value,
                time = timeNow,
                records = currentRecords,
                summary = summary
            )
        } else {
            WhatsAppUtils.generateFullAttendanceReport(
                className = _className.value,
                waliKelasName = _waliKelasName.value,
                reporterRole = reporterRole,
                reporterName = reporterName,
                dbDate = _selectedDate.value,
                time = timeNow,
                records = currentRecords,
                summary = summary
            )
        }

        WhatsAppUtils.sendReportViaWhatsApp(
            context = context,
            phoneNumber = _waliKelasPhone.value,
            message = textReport
        )
    }

    fun getPreviewReport(absentOnly: Boolean): String {
        val currentRecords = recordsForSelectedDate.value
        val summary = attendanceSummary.value
        val role = _reporterRole.value
        val reporterRole = role.displayName
        val reporterName = _reporterName.value.ifBlank { role.displayName }
        val timeNow = DateTimeUtils.getCurrentTime()

        return if (absentOnly) {
            WhatsAppUtils.generateAbsentOnlyReport(
                className = _className.value,
                waliKelasName = _waliKelasName.value,
                reporterRole = reporterRole,
                reporterName = reporterName,
                dbDate = _selectedDate.value,
                time = timeNow,
                records = currentRecords,
                summary = summary
            )
        } else {
            WhatsAppUtils.generateFullAttendanceReport(
                className = _className.value,
                waliKelasName = _waliKelasName.value,
                reporterRole = reporterRole,
                reporterName = reporterName,
                dbDate = _selectedDate.value,
                time = timeNow,
                records = currentRecords,
                summary = summary
            )
        }
    }

    // --- Settings & Credential Management ---

    fun updateClassInfo(className: String, waliKelasName: String, waliKelasPhone: String) {
        userPrefs.className = className
        userPrefs.waliKelasName = waliKelasName
        userPrefs.waliKelasPhone = waliKelasPhone
        _className.value = userPrefs.className
        _waliKelasName.value = userPrefs.waliKelasName
        _waliKelasPhone.value = userPrefs.waliKelasPhone
        _feedbackMessage.value = "Pengaturan kelas berhasil disimpan."
    }

    fun updateAdminPassword(newPassword: String): Boolean {
        if (newPassword.isBlank()) return false
        userPrefs.adminPassword = newPassword
        _feedbackMessage.value = "Password admin berhasil diperbarui."
        return true
    }

    fun updateKetuaCredentials(name: String, pin: String): Boolean {
        if (pin.isBlank() || name.isBlank()) return false
        userPrefs.ketuaName = name
        userPrefs.ketuaPin = pin
        _ketuaName.value = name
        _feedbackMessage.value = "Akun Ketua Kelas berhasil diperbarui."
        return true
    }

    fun updateWakilCredentials(name: String, pin: String): Boolean {
        if (pin.isBlank() || name.isBlank()) return false
        userPrefs.wakilName = name
        userPrefs.wakilPin = pin
        _wakilName.value = name
        _feedbackMessage.value = "Akun Wakil Ketua Kelas berhasil diperbarui."
        return true
    }

    // --- Backup & Restore ---

    suspend fun exportBackupJson(): String {
        val studentList = repository.getAllStudentsList()
        val recordList = repository.getAllRecordsList()
        return BackupRestoreUtils.createBackupJson(
            students = studentList,
            records = recordList,
            className = _className.value,
            waliKelasName = _waliKelasName.value,
            waliKelasPhone = _waliKelasPhone.value
        )
    }

    fun restoreFromBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val parseResult = BackupRestoreUtils.parseBackupJson(jsonString)
            if (parseResult.isSuccess) {
                val backupData = parseResult.getOrThrow()
                repository.restoreBackup(backupData)

                backupData.className?.let {
                    userPrefs.className = it
                    _className.value = it
                }
                backupData.waliKelasName?.let {
                    userPrefs.waliKelasName = it
                    _waliKelasName.value = it
                }
                backupData.waliKelasPhone?.let {
                    userPrefs.waliKelasPhone = it
                    _waliKelasPhone.value = it
                }

                refreshRecordsForDate(_selectedDate.value)
                onResult(
                    true,
                    "Berhasil restore ${backupData.students.size} siswa dan ${backupData.records.size} catatan absensi!"
                )
            } else {
                onResult(false, "Format backup tidak valid: ${parseResult.exceptionOrNull()?.message}")
            }
        }
    }
}
