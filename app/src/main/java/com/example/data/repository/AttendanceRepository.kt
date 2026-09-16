package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.StudentEntity
import com.example.domain.model.AttendanceStatus
import com.example.utils.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AttendanceRepository(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao
) {
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    val allRecords: Flow<List<AttendanceRecordEntity>> = attendanceDao.getAllRecords()

    fun getRecordsForDate(date: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsByDate(date)
    }

    suspend fun getAllStudentsList(): List<StudentEntity> = withContext(Dispatchers.IO) {
        studentDao.getAllStudentsList()
    }

    suspend fun getAllRecordsList(): List<AttendanceRecordEntity> = withContext(Dispatchers.IO) {
        attendanceDao.getAllRecordsList()
    }

    /**
     * Initializes attendance records for a given date if not already populated.
     * Default status is ALFA as explicitly requested: "dengan isian default nya adalah alfa".
     */
    suspend fun ensureRecordsForDate(date: String, recordedBy: String) = withContext(Dispatchers.IO) {
        val existing = attendanceDao.getRecordsByDateList(date)
        val students = studentDao.getAllStudentsList()
        if (students.isEmpty()) return@withContext

        val existingStudentIds = existing.map { it.studentId }.toSet()
        val missingStudents = students.filterNot { it.id in existingStudentIds }

        if (missingStudents.isNotEmpty()) {
            val newRecords = missingStudents.map { student ->
                AttendanceRecordEntity(
                    date = date,
                    studentId = student.id,
                    studentName = student.name,
                    attendanceNumber = student.attendanceNumber,
                    status = AttendanceStatus.ALFA.name, // DEFAULT IS ALFA
                    note = "",
                    recordedBy = recordedBy,
                    updatedAt = System.currentTimeMillis()
                )
            }
            attendanceDao.insertRecords(newRecords)
        }
    }

    /**
     * Update individual student's attendance status.
     */
    suspend fun updateStatus(
        date: String,
        studentId: Long,
        status: AttendanceStatus,
        recordedBy: String
    ) = withContext(Dispatchers.IO) {
        attendanceDao.updateRecordStatus(
            date = date,
            studentId = studentId,
            status = status.name,
            recordedBy = recordedBy,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Batch update all students' status for a specific date (e.g. Set all to BERANGKAT or ALFA).
     */
    suspend fun setAllStatus(
        date: String,
        status: AttendanceStatus,
        recordedBy: String
    ) = withContext(Dispatchers.IO) {
        val existing = attendanceDao.getRecordsByDateList(date)
        val updated = existing.map {
            it.copy(
                status = status.name,
                recordedBy = recordedBy,
                updatedAt = System.currentTimeMillis()
            )
        }
        attendanceDao.insertRecords(updated)
    }

    suspend fun updateRecordNote(date: String, studentId: Long, note: String) = withContext(Dispatchers.IO) {
        attendanceDao.updateRecordNote(date, studentId, note)
    }

    /**
     * Imports students from comma-separated text:
     * "memasukkan nama anak pakai teks, untuk memisahkan data pakai coma ','"
     * "data yang di minta adalah nama, untuk no absen otomatis terisi sesuai urutan nama secara abjad"
     */
    suspend fun importStudentsFromCommaSeparated(
        rawText: String,
        appendMode: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val namesFromInput = rawText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (namesFromInput.isEmpty()) return@withContext 0

        val existingNames = if (appendMode) {
            studentDao.getAllStudentsList().map { it.name }
        } else {
            emptyList()
        }

        // Combine and distinct case-insensitively
        val allUniqueNames = (existingNames + namesFromInput)
            .distinctBy { it.lowercase() }
            // Sort alphabetically A to Z
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        // Clear and rebuild with sequential attendance numbers
        studentDao.deleteAllStudents()
        val entities = allUniqueNames.mapIndexed { index, name ->
            StudentEntity(
                name = name,
                attendanceNumber = index + 1
            )
        }
        studentDao.insertStudents(entities)
        entities.size
    }

    suspend fun addSingleStudent(name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext false

        val existing = studentDao.getAllStudentsList()
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return@withContext false
        }

        val allNames = (existing.map { it.name } + trimmed).sortedWith(String.CASE_INSENSITIVE_ORDER)
        studentDao.deleteAllStudents()
        val entities = allNames.mapIndexed { index, n ->
            StudentEntity(
                name = n,
                attendanceNumber = index + 1
            )
        }
        studentDao.insertStudents(entities)
        true
    }

    suspend fun deleteStudent(studentId: Long) = withContext(Dispatchers.IO) {
        val existing = studentDao.getAllStudentsList().filterNot { it.id == studentId }
        val allNames = existing.map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER)

        studentDao.deleteAllStudents()
        val entities = allNames.mapIndexed { index, n ->
            StudentEntity(
                name = n,
                attendanceNumber = index + 1
            )
        }
        studentDao.insertStudents(entities)
    }

    suspend fun restoreBackup(backupData: BackupData) = withContext(Dispatchers.IO) {
        studentDao.deleteAllStudents()
        attendanceDao.deleteAllRecords()

        val sortedStudents = backupData.students
            .distinctBy { it.name.lowercase() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .mapIndexed { index, student ->
                StudentEntity(
                    id = 0,
                    name = student.name,
                    attendanceNumber = index + 1
                )
            }
        studentDao.insertStudents(sortedStudents)

        if (backupData.records.isNotEmpty()) {
            attendanceDao.insertRecords(backupData.records)
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        studentDao.deleteAllStudents()
        attendanceDao.deleteAllRecords()
    }
}
