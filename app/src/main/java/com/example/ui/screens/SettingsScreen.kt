package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSession: UserSession?,
    className: String,
    waliKelasName: String,
    waliKelasPhone: String,
    ketuaName: String,
    wakilName: String,
    onUpdateClassInfo: (String, String, String) -> Unit,
    onUpdateAdminPassword: (String) -> Boolean,
    onUpdateKetuaCredentials: (String, String) -> Boolean,
    onUpdateWakilCredentials: (String, String) -> Boolean,
    onTriggerBackup: () -> Unit,
    onTriggerRestore: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Class Info Form
    var classInput by remember(className) { mutableStateOf(className) }
    var waliNameInput by remember(waliKelasName) { mutableStateOf(waliKelasName) }
    var waliPhoneInput by remember(waliKelasPhone) { mutableStateOf(waliKelasPhone) }

    // Dialog states for credentials
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var newAdminPassword by remember { mutableStateOf("") }

    var showKetuaDialog by remember { mutableStateOf(false) }
    var ketuaNameInput by remember(ketuaName) { mutableStateOf(ketuaName) }
    var ketuaPinInput by remember { mutableStateOf("") }

    var showWakilDialog by remember { mutableStateOf(false) }
    var wakilNameInput by remember(wakilName) { mutableStateOf(wakilName) }
    var wakilPinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Session Info
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSession?.displayName ?: "Pengguna",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hak Akses: ${currentSession?.role?.displayName ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("button_logout_settings")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keluar")
                    }
                }
            }

            // Section: Profil Kelas & Wali Kelas
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Data Kelas & Wali Kelas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = classInput,
                        onValueChange = { classInput = it },
                        label = { Text("Nama Kelas") },
                        placeholder = { Text("Contoh: XII RPL 1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_class_name_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = waliNameInput,
                        onValueChange = { waliNameInput = it },
                        label = { Text("Nama Wali Kelas") },
                        placeholder = { Text("Contoh: Drs. Bambang S.") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_wali_name_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = waliPhoneInput,
                        onValueChange = { waliPhoneInput = it },
                        label = { Text("Nomor WhatsApp Wali Kelas") },
                        placeholder = { Text("Contoh: 081234567890 / 6281234567890") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_wali_phone_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            onUpdateClassInfo(classInput, waliNameInput, waliPhoneInput)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_class_info_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Informasi Kelas")
                    }
                }
            }

            // Section: Keamanan & Hak Akses
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hak Akses & Kata Sandi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Atur password dan PIN untuk membedakan akses Admin, Ketua Kelas, dan Wakil Ketua Kelas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Change Admin Password
                    OutlinedButton(
                        onClick = { showAdminPasswordDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_change_admin_password")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Password Admin")
                    }

                    // Manage Ketua Kelas
                    OutlinedButton(
                        onClick = { showKetuaDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_manage_ketua")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Akun & PIN Ketua Kelas")
                    }

                    // Manage Wakil Ketua Kelas
                    OutlinedButton(
                        onClick = { showWakilDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_manage_wakil")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Akun & PIN Wakil Ketua Kelas")
                    }
                }
            }

            // Section: Backup & Restore Data
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup & Restore Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Cadangkan (backup) seluruh data nama anak, nomor absen, dan riwayat absensi, atau pulihkan (restore) dari cadangan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTriggerBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_open_backup_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Data")
                        }

                        OutlinedButton(
                            onClick = onTriggerRestore,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_open_restore_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Data")
                        }
                    }
                }
            }
        }

        // Dialog: Change Admin Password
        if (showAdminPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showAdminPasswordDialog = false },
                title = { Text("Ubah Password Admin") },
                text = {
                    Column {
                        Text("Masukkan password baru untuk akun Administrator:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newAdminPassword,
                            onValueChange = { newAdminPassword = it },
                            placeholder = { Text("Password baru...") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_new_admin_password")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAdminPassword.isNotBlank()) {
                                onUpdateAdminPassword(newAdminPassword)
                                newAdminPassword = ""
                                showAdminPasswordDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_admin_password_button")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminPasswordDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog: Manage Ketua Kelas
        if (showKetuaDialog) {
            AlertDialog(
                onDismissRequest = { showKetuaDialog = false },
                title = { Text("Pengaturan Ketua Kelas") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nama dan PIN login untuk Ketua Kelas:")
                        OutlinedTextField(
                            value = ketuaNameInput,
                            onValueChange = { ketuaNameInput = it },
                            label = { Text("Nama Ketua Kelas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("field_ketua_name")
                        )
                        OutlinedTextField(
                            value = ketuaPinInput,
                            onValueChange = { ketuaPinInput = it },
                            label = { Text("PIN / Password Baru") },
                            placeholder = { Text("Biarkan kosong jika tidak diubah") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("field_ketua_pin")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pinToUse = if (ketuaPinInput.isNotBlank()) ketuaPinInput else "1111"
                            onUpdateKetuaCredentials(ketuaNameInput, pinToUse)
                            showKetuaDialog = false
                        },
                        modifier = Modifier.testTag("confirm_ketua_save_button")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showKetuaDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog: Manage Wakil Ketua Kelas
        if (showWakilDialog) {
            AlertDialog(
                onDismissRequest = { showWakilDialog = false },
                title = { Text("Pengaturan Wakil Ketua") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nama dan PIN login untuk Wakil Ketua Kelas:")
                        OutlinedTextField(
                            value = wakilNameInput,
                            onValueChange = { wakilNameInput = it },
                            label = { Text("Nama Wakil Ketua Kelas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("field_wakil_name")
                        )
                        OutlinedTextField(
                            value = wakilPinInput,
                            onValueChange = { wakilPinInput = it },
                            label = { Text("PIN / Password Baru") },
                            placeholder = { Text("Biarkan kosong jika tidak diubah") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("field_wakil_pin")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pinToUse = if (wakilPinInput.isNotBlank()) wakilPinInput else "2222"
                            onUpdateWakilCredentials(wakilNameInput, pinToUse)
                            showWakilDialog = false
                        },
                        modifier = Modifier.testTag("confirm_wakil_save_button")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWakilDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
