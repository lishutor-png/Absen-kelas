package com.example.utils

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.StudentEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val students: List<StudentEntity>,
    val records: List<AttendanceRecordEntity>,
    val className: String?,
    val waliKelasName: String?,
    val waliKelasPhone: String?,
    val exportDate: String
)

object BackupRestoreUtils {

    /**
     * Converts students, attendance records, and settings to a JSON string.
     */
    fun createBackupJson(
        students: List<StudentEntity>,
        records: List<AttendanceRecordEntity>,
        className: String,
        waliKelasName: String,
        waliKelasPhone: String
    ): String {
        val root = JSONObject()
        root.put("appName", "Lapor Absen")
        root.put("version", 1)
        root.put("exportDate", DateTimeUtils.getCurrentTime())

        val settingsObj = JSONObject()
        settingsObj.put("className", className)
        settingsObj.put("waliKelasName", waliKelasName)
        settingsObj.put("waliKelasPhone", waliKelasPhone)
        root.put("settings", settingsObj)

        val studentsArray = JSONArray()
        students.forEach { s ->
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("name", s.name)
            sObj.put("attendanceNumber", s.attendanceNumber)
            studentsArray.put(sObj)
        }
        root.put("students", studentsArray)

        val recordsArray = JSONArray()
        records.forEach { r ->
            val rObj = JSONObject()
            rObj.put("date", r.date)
            rObj.put("studentId", r.studentId)
            rObj.put("studentName", r.studentName)
            rObj.put("attendanceNumber", r.attendanceNumber)
            rObj.put("status", r.status)
            rObj.put("note", r.note)
            rObj.put("recordedBy", r.recordedBy)
            rObj.put("updatedAt", r.updatedAt)
            recordsArray.put(rObj)
        }
        root.put("attendanceRecords", recordsArray)

        return root.toString(2)
    }

    /**
     * Parses JSON string into BackupData.
     */
    fun parseBackupJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString.trim())

            var className: String? = null
            var waliKelasName: String? = null
            var waliKelasPhone: String? = null

            if (root.has("settings")) {
                val settings = root.getJSONObject("settings")
                className = settings.optString("className", null)
                waliKelasName = settings.optString("waliKelasName", null)
                waliKelasPhone = settings.optString("waliKelasPhone", null)
            }

            val studentsList = mutableListOf<StudentEntity>()
            if (root.has("students")) {
                val arr = root.getJSONArray("students")
                for (i in 0 until arr.length()) {
                    val sObj = arr.getJSONObject(i)
                    studentsList.add(
                        StudentEntity(
                            id = sObj.optLong("id", 0),
                            name = sObj.getString("name"),
                            attendanceNumber = sObj.optInt("attendanceNumber", i + 1)
                        )
                    )
                }
            }

            val recordsList = mutableListOf<AttendanceRecordEntity>()
            if (root.has("attendanceRecords")) {
                val arr = root.getJSONArray("attendanceRecords")
                for (i in 0 until arr.length()) {
                    val rObj = arr.getJSONObject(i)
                    recordsList.add(
                        AttendanceRecordEntity(
                            id = 0,
                            date = rObj.getString("date"),
                            studentId = rObj.optLong("studentId", 0),
                            studentName = rObj.getString("studentName"),
                            attendanceNumber = rObj.optInt("attendanceNumber", 1),
                            status = rObj.getString("status"),
                            note = rObj.optString("note", ""),
                            recordedBy = rObj.optString("recordedBy", ""),
                            updatedAt = rObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            Result.success(
                BackupData(
                    students = studentsList,
                    records = recordsList,
                    className = className,
                    waliKelasName = waliKelasName,
                    waliKelasPhone = waliKelasPhone,
                    exportDate = root.optString("exportDate", "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
