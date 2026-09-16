package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.StudentEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceSummary
import com.example.domain.model.UserSession
import com.example.ui.theme.StatusAlfa
import com.example.ui.theme.StatusAlfaBg
import com.example.ui.theme.StatusBerangkat
import com.example.ui.theme.StatusBerangkatBg
import com.example.ui.theme.StatusIzin
import com.example.ui.theme.StatusIzinBg
import com.example.ui.theme.StatusSakit
import com.example.ui.theme.StatusSakitBg
import com.example.utils.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    currentSession: UserSession?,
    className: String,
    selectedDate: String,
    students: List<StudentEntity>,
    records: List<AttendanceRecordEntity>,
    summary: AttendanceSummary,
    onStatusChange: (Long, AttendanceStatus) -> Unit,
    onNoteChange: (Long, String) -> Unit,
    onMarkAllBerangkat: () -> Unit,
    onResetAllAlfa: () -> Unit,
    onDateSelected: (String) -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf(DateTimeUtils.getCurrentTime()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // Note dialog state
    var editingStudentNote by remember { mutableStateOf<Pair<Long, String>?>(null) } // studentId to initialNote
    var noteInputText by remember { mutableStateOf("") }

    // Periodically update current clock string
    LaunchedEffect(Unit) {
        currentTimeString = DateTimeUtils.getCurrentTime()
    }

    // Map of records by studentId for fast lookup
    val recordMap = remember(records) {
        records.associateBy { it.studentId }
    }

    // Filtered student list
    val filteredStudents = remember(students, recordMap, searchQuery, filterStatus) {
        students.filter { student ->
            val matchesQuery = searchQuery.isBlank() || student.name.contains(searchQuery, ignoreCase = true)
            val currentStatus = AttendanceStatus.fromString(
                recordMap[student.id]?.status ?: AttendanceStatus.ALFA.name
            )
            val matchesFilter = filterStatus == null || currentStatus == filterStatus
            matchesQuery && matchesFilter
        }
    }

    // Date Picker Dialog trigger
    val openDatePicker = {
        try {
            val parts = selectedDate.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
            val day = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(
                context,
                { _, y, m, d ->
                    val newDateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                    onDateSelected(newDateStr)
                },
                year,
                month,
                day
            )
            dpd.show()
        } catch (_: Exception) {
            // fallback
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lapor Absen - $className",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val roleLabel = currentSession?.role?.displayName ?: "Ketua Kelas"
                            val nameLabel = currentSession?.displayName
                            val subtitleText = if (nameLabel.isNullOrBlank() || nameLabel == roleLabel) {
                                "Pelapor: $roleLabel"
                            } else {
                                "Pelapor: $roleLabel ($nameLabel)"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToStudents,
                        modifier = Modifier.testTag("action_manage_students")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Kelola Siswa"
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (students.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToReport,
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    text = { Text("Lapor ke Wali Kelas") },
                    containerColor = Color(0xFF25D366), // WhatsApp Green
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_report_whatsapp")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date & Time Bar (Otomatis dari HP)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date picker clicker
                    Row(
                        modifier = Modifier
                            .clickable { openDatePicker() }
                            .testTag("button_change_date"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateTimeUtils.formatDisplayDate(selectedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Auto Phone Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentTimeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-time Summary Counters Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rekapitulasi Kehadiran Hari Ini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${summary.total} Siswa",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(
                            label = "Berangkat",
                            count = summary.berangkat,
                            textColor = StatusBerangkat,
                            bgColor = StatusBerangkatBg,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryPill(
                            label = "Sakit",
                            count = summary.sakit,
                            textColor = StatusSakit,
                            bgColor = StatusSakitBg,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryPill(
                            label = "Izin",
                            count = summary.izin,
                            textColor = StatusIzin,
                            bgColor = StatusIzinBg,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryPill(
                            label = "Alfa",
                            count = summary.alfa,
                            textColor = StatusAlfa,
                            bgColor = StatusAlfaBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Batch Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkAllBerangkat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_mark_all_berangkat"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusBerangkat
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Semua Berangkat", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onResetAllAlfa,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_reset_all_alfa"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset ke Alfa (Def)", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("Semua", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_all")
                )
                FilterChip(
                    selected = filterStatus == AttendanceStatus.BERANGKAT,
                    onClick = { filterStatus = AttendanceStatus.BERANGKAT },
                    label = { Text("Berangkat", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_berangkat")
                )
                FilterChip(
                    selected = filterStatus == AttendanceStatus.SAKIT,
                    onClick = { filterStatus = AttendanceStatus.SAKIT },
                    label = { Text("Sakit", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_sakit")
                )
                FilterChip(
                    selected = filterStatus == AttendanceStatus.IZIN,
                    onClick = { filterStatus = AttendanceStatus.IZIN },
                    label = { Text("Izin", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_izin")
                )
                FilterChip(
                    selected = filterStatus == AttendanceStatus.ALFA,
                    onClick = { filterStatus = AttendanceStatus.ALFA },
                    label = { Text("Alfa", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_alfa")
                )
            }

            // Student Attendance List
            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Belum Ada Data Siswa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Masukkan nama-nama siswa kelas menggunakan teks dengan pemisah koma (,). Sistem akan otomatis mengurutkan nama sesuai abjad.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToStudents,
                                modifier = Modifier.testTag("button_start_add_students"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Input Data Nama Siswa")
                            }
                        }
                    }
                }
            } else if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada siswa dengan filter saat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(
                        items = filteredStudents,
                        key = { it.id }
                    ) { student ->
                        val currentRecord = recordMap[student.id]
                        val currentStatus = AttendanceStatus.fromString(
                            currentRecord?.status ?: AttendanceStatus.ALFA.name
                        )
                        val note = currentRecord?.note ?: ""

                        StudentAttendanceCard(
                            student = student,
                            currentStatus = currentStatus,
                            note = note,
                            onStatusSelected = { newStatus ->
                                onStatusChange(student.id, newStatus)
                            },
                            onEditNote = {
                                editingStudentNote = Pair(student.id, note)
                                noteInputText = note
                            }
                        )
                    }
                }
            }
        }

        // Dialog to edit note (e.g. alasan sakit / izin)
        editingStudentNote?.let { (studentId, _) ->
            val studentName = students.find { it.id == studentId }?.name ?: "Siswa"
            AlertDialog(
                onDismissRequest = { editingStudentNote = null },
                title = { Text("Keterangan untuk $studentName") },
                text = {
                    Column {
                        Text(
                            text = "Tambahkan alasan atau keterangan (misal: demam, rawat jalan, izin lomba):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteInputText,
                            onValueChange = { noteInputText = it },
                            placeholder = { Text("Tulis keterangan...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_student_note")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onNoteChange(studentId, noteInputText.trim())
                            editingStudentNote = null
                        },
                        modifier = Modifier.testTag("save_student_note_button")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingStudentNote = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun SummaryPill(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun StudentAttendanceCard(
    student: StudentEntity,
    currentStatus: AttendanceStatus,
    note: String,
    onStatusSelected: (AttendanceStatus) -> Unit,
    onEditNote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_card_${student.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Number + Name + Note button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${student.attendanceNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isNotBlank()) {
                        Text(
                            text = "Ket: $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit Note Button
                IconButton(
                    onClick = onEditNote,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("button_note_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Beri Keterangan",
                        tint = if (note.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Status Buttons: Berangkat, Sakit, Izin, Alfa (Default: Alfa)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusToggleButton(
                    label = "Alfa",
                    isSelected = currentStatus == AttendanceStatus.ALFA,
                    activeColor = StatusAlfa,
                    activeBgColor = StatusAlfaBg,
                    onClick = { onStatusSelected(AttendanceStatus.ALFA) },
                    modifier = Modifier.weight(1f),
                    testTag = "status_alfa_${student.id}"
                )

                StatusToggleButton(
                    label = "Berangkat",
                    isSelected = currentStatus == AttendanceStatus.BERANGKAT,
                    activeColor = StatusBerangkat,
                    activeBgColor = StatusBerangkatBg,
                    onClick = { onStatusSelected(AttendanceStatus.BERANGKAT) },
                    modifier = Modifier.weight(1.2f),
                    testTag = "status_berangkat_${student.id}"
                )

                StatusToggleButton(
                    label = "Sakit",
                    isSelected = currentStatus == AttendanceStatus.SAKIT,
                    activeColor = StatusSakit,
                    activeBgColor = StatusSakitBg,
                    onClick = { onStatusSelected(AttendanceStatus.SAKIT) },
                    modifier = Modifier.weight(1f),
                    testTag = "status_sakit_${student.id}"
                )

                StatusToggleButton(
                    label = "Izin",
                    isSelected = currentStatus == AttendanceStatus.IZIN,
                    activeColor = StatusIzin,
                    activeBgColor = StatusIzinBg,
                    onClick = { onStatusSelected(AttendanceStatus.IZIN) },
                    modifier = Modifier.weight(1f),
                    testTag = "status_izin_${student.id}"
                )
            }
        }
    }
}

@Composable
fun StatusToggleButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeBgColor else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
