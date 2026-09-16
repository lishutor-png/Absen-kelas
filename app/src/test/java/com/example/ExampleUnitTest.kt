package com.example

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.StudentEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceSummary
import com.example.utils.BackupRestoreUtils
import com.example.utils.WhatsAppUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun testAlphabeticalSortingAndAutoNumbering() {
    val rawCommaText = "Zaki, Budi, Anisa, Citra, Dedi"
    val names = rawCommaText.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    assertEquals(listOf("Anisa", "Budi", "Citra", "Dedi", "Zaki"), names)

    val studentsWithNumber = names.mapIndexed { index, name ->
        StudentEntity(name = name, attendanceNumber = index + 1)
    }

    assertEquals(1, studentsWithNumber[0].attendanceNumber)
    assertEquals("Anisa", studentsWithNumber[0].name)

    assertEquals(2, studentsWithNumber[1].attendanceNumber)
    assertEquals("Budi", studentsWithNumber[1].name)

    assertEquals(5, studentsWithNumber[4].attendanceNumber)
    assertEquals("Zaki", studentsWithNumber[4].name)
  }

  @Test
  fun testPhoneNumberNormalization() {
    assertEquals("628123456789", WhatsAppUtils.normalizePhoneNumber("08123456789"))
    assertEquals("628123456789", WhatsAppUtils.normalizePhoneNumber("+628123456789"))
    assertEquals("628123456789", WhatsAppUtils.normalizePhoneNumber("0812-3456-789"))
  }

  @Test
  fun testAbsentOnlyReportGeneration() {
    val records = listOf(
        AttendanceRecordEntity(
            studentId = 1,
            studentName = "Anisa",
            attendanceNumber = 1,
            date = "2026-09-16",
            status = AttendanceStatus.BERANGKAT.name
        ),
        AttendanceRecordEntity(
            studentId = 2,
            studentName = "Budi",
            attendanceNumber = 2,
            date = "2026-09-16",
            status = AttendanceStatus.SAKIT.name,
            note = "Demam"
        ),
        AttendanceRecordEntity(
            studentId = 3,
            studentName = "Citra",
            attendanceNumber = 3,
            date = "2026-09-16",
            status = AttendanceStatus.ALFA.name
        )
    )

    val summary = AttendanceSummary(total = 3, berangkat = 1, sakit = 1, izin = 0, alfa = 1)

    val report = WhatsAppUtils.generateAbsentOnlyReport(
        className = "XII RPL 1",
        waliKelasName = "Pak Guru",
        reporterRole = "Ketua Kelas",
        reporterName = "Ahmad",
        dbDate = "2026-09-16",
        time = "07:30 WIB",
        records = records,
        summary = summary
    )

    assertTrue(report.contains("XII RPL 1"))
    assertTrue(report.contains("Pak Guru"))
    assertTrue(report.contains("Budi"))
    assertTrue(report.contains("SAKIT"))
    assertTrue(report.contains("Citra"))
    assertTrue(report.contains("ALFA"))
    // Anisa is present so she shouldn't be in the absent-only list
    assertTrue(!report.contains("1. *Anisa*"))
  }

  @Test
  fun testBackupAndRestoreJson() {
    val originalStudents = listOf(
        StudentEntity(id = 1, name = "Anisa", attendanceNumber = 1),
        StudentEntity(id = 2, name = "Budi", attendanceNumber = 2)
    )
    val originalRecords = listOf(
        AttendanceRecordEntity(
            id = 1,
            date = "2026-09-16",
            studentId = 1,
            studentName = "Anisa",
            attendanceNumber = 1,
            status = "BERANGKAT"
        )
    )

    val json = BackupRestoreUtils.createBackupJson(
        students = originalStudents,
        records = originalRecords,
        className = "XII IPA 1",
        waliKelasName = "Bu Guru",
        waliKelasPhone = "08123456789"
    )

    val result = BackupRestoreUtils.parseBackupJson(json)
    assertTrue(result.isSuccess)

    val backupData = result.getOrThrow()
    assertEquals(2, backupData.students.size)
    assertEquals("Anisa", backupData.students[0].name)
    assertEquals(1, backupData.records.size)
    assertEquals("XII IPA 1", backupData.className)
    assertEquals("Bu Guru", backupData.waliKelasName)
    assertEquals("08123456789", backupData.waliKelasPhone)
  }
}
