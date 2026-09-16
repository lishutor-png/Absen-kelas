package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceSummary
import java.net.URLEncoder

object WhatsAppUtils {

    /**
     * Normalizes an Indonesian phone number:
     * e.g. "08123456789" -> "628123456789"
     * "+628123456789" -> "628123456789"
     */
    fun normalizePhoneNumber(phone: String): String {
        var clean = phone.replace(Regex("[^0-9+]"), "")
        if (clean.startsWith("+")) {
            clean = clean.substring(1)
        }
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        }
        return clean
    }

    /**
     * Generates report text for only students who are absent (Alfa, Sakit, Izin).
     */
    fun generateAbsentOnlyReport(
        className: String,
        waliKelasName: String,
        reporterRole: String,
        reporterName: String,
        dbDate: String,
        time: String,
        records: List<AttendanceRecordEntity>,
        summary: AttendanceSummary
    ): String {
        val displayDate = DateTimeUtils.formatDisplayDate(dbDate)
        val nonAttending = records.filter { it.status != AttendanceStatus.BERANGKAT.name }

        val sb = StringBuilder()
        sb.append("📋 *LAPORAN KETIDAKHADIRAN SISWA*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏫 *Kelas:* $className\n")
        sb.append("👤 *Wali Kelas:* $waliKelasName\n")
        sb.append("📅 *Hari/Tanggal:* $displayDate\n")
        sb.append("⏰ *Pukul:* $time\n")
        sb.append("✍️ *Pelapor:* $reporterName ($reporterRole)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("📊 *RINGKASAN KEHADIRAN:*\n")
        sb.append("• Total Siswa: ${summary.total} anak\n")
        sb.append("• Berangkat (Hadir): ${summary.berangkat} anak\n")
        sb.append("• Sakit: ${summary.sakit} anak\n")
        sb.append("• Izin: ${summary.izin} anak\n")
        sb.append("• Alfa: ${summary.alfa} anak\n\n")

        sb.append("📌 *DAFTAR SISWA TIDAK BERANGKAT:*\n")
        if (nonAttending.isEmpty()) {
            sb.append("✨ *Nihil* - Semua siswa berangkat (hadir lengkap).\n")
        } else {
            nonAttending.forEach { record ->
                val statusSymbol = when (record.status) {
                    AttendanceStatus.SAKIT.name -> "🟡 SAKIT"
                    AttendanceStatus.IZIN.name -> "🔵 IZIN"
                    else -> "🔴 ALFA"
                }
                val noteText = if (record.note.isNotBlank()) " (Ket: ${record.note})" else ""
                sb.append("${record.attendanceNumber}. *${record.studentName}* ➔ $statusSymbol$noteText\n")
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("_Laporan dibuat otomatis oleh Aplikasi Lapor Absen_")
        return sb.toString()
    }

    /**
     * Generates report text for all students.
     */
    fun generateFullAttendanceReport(
        className: String,
        waliKelasName: String,
        reporterRole: String,
        reporterName: String,
        dbDate: String,
        time: String,
        records: List<AttendanceRecordEntity>,
        summary: AttendanceSummary
    ): String {
        val displayDate = DateTimeUtils.formatDisplayDate(dbDate)

        val sb = StringBuilder()
        sb.append("📋 *REKAP LENGKAP ABSENSI KELAS*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏫 *Kelas:* $className\n")
        sb.append("👤 *Wali Kelas:* $waliKelasName\n")
        sb.append("📅 *Hari/Tanggal:* $displayDate\n")
        sb.append("⏰ *Pukul:* $time\n")
        sb.append("✍️ *Pelapor:* $reporterName ($reporterRole)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("📊 *STATISTIK KEHADIRAN:*\n")
        sb.append("✅ Berangkat: ${summary.berangkat} anak\n")
        sb.append("🟡 Sakit: ${summary.sakit} anak\n")
        sb.append("🔵 Izin: ${summary.izin} anak\n")
        sb.append("🔴 Alfa: ${summary.alfa} anak\n")
        sb.append("👥 Total Siswa: ${summary.total} anak\n\n")

        sb.append("📝 *DAFTAR LENGKAP ABSENSI:*\n")
        records.forEach { record ->
            val statusTag = when (record.status) {
                AttendanceStatus.BERANGKAT.name -> "[HADIR]"
                AttendanceStatus.SAKIT.name -> "[SAKIT]"
                AttendanceStatus.IZIN.name -> "[IZIN]"
                else -> "[ALFA]"
            }
            val noteText = if (record.note.isNotBlank()) " (${record.note})" else ""
            sb.append("${record.attendanceNumber}. ${record.studentName} $statusTag$noteText\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("_Laporan dibuat otomatis oleh Aplikasi Lapor Absen_")
        return sb.toString()
    }

    /**
     * Dispatches report via WhatsApp API or Intent.
     */
    fun sendReportViaWhatsApp(
        context: Context,
        phoneNumber: String,
        message: String
    ) {
        val cleanPhone = normalizePhoneNumber(phoneNumber)
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Try standard WhatsApp first
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Try WhatsApp Business
                intent.setPackage("com.whatsapp.w4b")
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback to browser or standard chooser
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }
            }
        } catch (e: Exception) {
            // General share fallback
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Kirim Rekap Absensi"))
            } catch (_: Exception) {
                copyToClipboard(context, message)
                Toast.makeText(context, "Gagal membuka WhatsApp. Teks telah disalin ke clipboard.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Laporan Absensi", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Teks laporan berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }
}
