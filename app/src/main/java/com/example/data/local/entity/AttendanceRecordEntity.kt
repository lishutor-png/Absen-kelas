package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["date", "studentId"], unique = true),
        Index(value = ["date"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val studentId: Long,
    val studentName: String,
    val attendanceNumber: Int,
    val status: String, // "ALFA", "BERANGKAT", "SAKIT", "IZIN"
    val note: String = "",
    val recordedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
