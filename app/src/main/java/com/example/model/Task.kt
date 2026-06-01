package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val categoryId: Int = 1, // 1 = Work, 2 = Study, 3 = Personal, 4 = Health
    val status: String = STATUS_PENDING, // "pending" | "completed" | "moved"
    val creationWeek: Int, // week number in the year (e.g. 1 to 52)
    val scheduledDate: String, // Format: YYYY-MM-DD (excellent for sorting and filtering)
    val reminderTime: String, // Format: HH:mm
    val originalDate: String? = null, // Tracking the original scheduled date if postponed/snoozed
    val proofImage: String? = null, // Lưu đường dẫn URI của ảnh sau khi chụp check-in (ví dụ: 'file://...')
    val completedAt: String? = null // Lưu chuỗi thời gian (HH:mm) lúc người dùng bấm hoàn thành để hiển thị đè lên ảnh
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_MOVED = "moved"
    }
}
