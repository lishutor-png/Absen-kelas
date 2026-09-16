package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BackupDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.RestoreDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentManagementScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN,
    ATTENDANCE,
    STUDENT_MANAGEMENT,
    REPORT,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                LaporAbsenApp()
            }
        }
    }
}

@Composable
fun LaporAbsenApp(
    viewModel: AttendanceViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val records by viewModel.recordsForSelectedDate.collectAsStateWithLifecycle()
    val summary by viewModel.attendanceSummary.collectAsStateWithLifecycle()

    val className by viewModel.className.collectAsStateWithLifecycle()
    val waliKelasName by viewModel.waliKelasName.collectAsStateWithLifecycle()
    val waliKelasPhone by viewModel.waliKelasPhone.collectAsStateWithLifecycle()
    val ketuaName by viewModel.ketuaName.collectAsStateWithLifecycle()
    val wakilName by viewModel.wakilName.collectAsStateWithLifecycle()

    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    // Screen navigation state
    var currentScreen by remember(currentSession) {
        mutableStateOf(if (currentSession == null) Screen.LOGIN else Screen.ATTENDANCE)
    }

    // Dialog states for Backup & Restore
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }

    // Show feedback snackbar when feedbackMessage changes
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }

    // Hardware back button navigation handling
    if (currentScreen != Screen.LOGIN && currentScreen != Screen.ATTENDANCE) {
        BackHandler {
            currentScreen = Screen.ATTENDANCE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.LOGIN -> {
                LoginScreen(
                    onLoginAttempt = { role, credential ->
                        viewModel.login(role, credential)
                    },
                    onLoginSuccess = { _ ->
                        currentScreen = Screen.ATTENDANCE
                    }
                )
            }

            Screen.ATTENDANCE -> {
                AttendanceScreen(
                    currentSession = currentSession,
                    className = className,
                    selectedDate = selectedDate,
                    students = students,
                    records = records,
                    summary = summary,
                    onStatusChange = { studentId, status ->
                        viewModel.updateStudentStatus(studentId, status)
                    },
                    onNoteChange = { studentId, note ->
                        viewModel.updateStudentNote(studentId, note)
                    },
                    onMarkAllBerangkat = {
                        viewModel.markAllBerangkat()
                    },
                    onResetAllAlfa = {
                        viewModel.resetAllToAlfa()
                    },
                    onDateSelected = { newDate ->
                        viewModel.setDate(newDate)
                    },
                    onNavigateToStudents = {
                        currentScreen = Screen.STUDENT_MANAGEMENT
                    },
                    onNavigateToReport = {
                        currentScreen = Screen.REPORT
                    },
                    onNavigateToSettings = {
                        currentScreen = Screen.SETTINGS
                    }
                )
            }

            Screen.STUDENT_MANAGEMENT -> {
                StudentManagementScreen(
                    students = students,
                    onImportStudents = { text, append ->
                        viewModel.importStudents(text, append)
                    },
                    onAddSingleStudent = { name ->
                        viewModel.addSingleStudent(name)
                    },
                    onDeleteStudent = { studentId ->
                        viewModel.deleteStudent(studentId)
                    },
                    onNavigateBack = {
                        currentScreen = Screen.ATTENDANCE
                    }
                )
            }

            Screen.REPORT -> {
                ReportScreen(
                    className = className,
                    waliKelasName = waliKelasName,
                    waliKelasPhone = waliKelasPhone,
                    attendanceSummary = summary,
                    getPreviewReport = { absentOnly ->
                        viewModel.getPreviewReport(absentOnly)
                    },
                    onSendWhatsApp = { context, absentOnly ->
                        viewModel.sendWhatsAppReport(context, absentOnly)
                    },
                    onNavigateBack = {
                        currentScreen = Screen.ATTENDANCE
                    },
                    onNavigateToSettings = {
                        currentScreen = Screen.SETTINGS
                    }
                )
            }

            Screen.SETTINGS -> {
                SettingsScreen(
                    currentSession = currentSession,
                    className = className,
                    waliKelasName = waliKelasName,
                    waliKelasPhone = waliKelasPhone,
                    ketuaName = ketuaName,
                    wakilName = wakilName,
                    onUpdateClassInfo = { cName, wName, wPhone ->
                        viewModel.updateClassInfo(cName, wName, wPhone)
                    },
                    onUpdateAdminPassword = { newPass ->
                        viewModel.updateAdminPassword(newPass)
                    },
                    onUpdateKetuaCredentials = { name, pin ->
                        viewModel.updateKetuaCredentials(name, pin)
                    },
                    onUpdateWakilCredentials = { name, pin ->
                        viewModel.updateWakilCredentials(name, pin)
                    },
                    onTriggerBackup = {
                        coroutineScope.launch {
                            backupJsonContent = viewModel.exportBackupJson()
                            showBackupDialog = true
                        }
                    },
                    onTriggerRestore = {
                        showRestoreDialog = true
                    },
                    onLogout = {
                        viewModel.logout()
                        currentScreen = Screen.LOGIN
                    },
                    onNavigateBack = {
                        currentScreen = Screen.ATTENDANCE
                    }
                )
            }
        }

        // Backup Modal Dialog
        if (showBackupDialog) {
            BackupDialog(
                backupJson = backupJsonContent,
                onDismiss = { showBackupDialog = false }
            )
        }

        // Restore Modal Dialog
        if (showRestoreDialog) {
            RestoreDialog(
                onRestore = { json ->
                    viewModel.restoreFromBackupJson(json) { success, msg ->
                        showRestoreDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                },
                onDismiss = { showRestoreDialog = false }
            )
        }
    }
}
