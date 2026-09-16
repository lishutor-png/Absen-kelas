package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY attendanceNumber ASC, studentName ASC")
    fun getRecordsByDate(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY attendanceNumber ASC, studentName ASC")
    suspend fun getRecordsByDateList(date: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, attendanceNumber ASC")
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, attendanceNumber ASC")
    suspend fun getAllRecordsList(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query("UPDATE attendance_records SET status = :status, recordedBy = :recordedBy, updatedAt = :updatedAt WHERE date = :date AND studentId = :studentId")
    suspend fun updateRecordStatus(
        date: String,
        studentId: Long,
        status: String,
        recordedBy: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE attendance_records SET note = :note WHERE date = :date AND studentId = :studentId")
    suspend fun updateRecordNote(date: String, studentId: Long, note: String)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteRecordsByDate(date: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()
}
