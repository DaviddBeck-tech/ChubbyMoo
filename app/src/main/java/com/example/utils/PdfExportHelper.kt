package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.model.Task
import com.example.model.Category
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import androidx.compose.ui.graphics.toArgb

object PdfExportHelper {
    fun exportWeeklyReport(context: Context, tasks: List<Task>): File? {
        if (tasks.isEmpty()) {
            Toast.makeText(context, "Không có công việc nào được tích chọn để xuất báo cáo!", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val pdfDocument = PdfDocument()
            // Standard A4 size is 595 x 842 points (72 points per inch)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            val paint = Paint()
            
            // Draw a cute decorative top borderline or gradient
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.parseColor("#E0BBE4") // Pastel Lavender
            canvas.drawRect(0f, 0f, 595f, 15f, paint)

            // Header Title
            paint.textSize = 20f
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.parseColor("#333333")
            canvas.drawText("BÁO CÁO TỔNG KẾT TUẦN 🌸", 40f, 60f, paint)
            
            // Subtitle
            paint.textSize = 11f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.parseColor("#7A7A7A")
            canvas.drawText("Trích xuất thông minh từ Lovely Scheduler", 40f, 82f, paint)
            
            // Separator double line
            paint.color = android.graphics.Color.parseColor("#E0BBE4")
            paint.strokeWidth = 2f
            canvas.drawLine(40f, 100f, 555f, 100f, paint)
            paint.color = android.graphics.Color.parseColor("#FFFFD1") // Yellow line double design
            canvas.drawLine(40f, 104f, 555f, 104f, paint)
            
            // Meta info on the right side
            paint.textSize = 10f
            paint.color = android.graphics.Color.parseColor("#333333")
            val todayStr = LocalDate.now().toString()
            canvas.drawText("Người lập: Thành viên Lovely", 380f, 55f, paint)
            canvas.drawText("Ngày tạo: $todayStr", 380f, 70f, paint)
            
            val completedCount = tasks.count { it.status == Task.STATUS_COMPLETED }
            val progressPercentage = if (tasks.isEmpty()) 0 else (completedCount.toFloat() / tasks.size * 100).toInt()
            canvas.drawText("Tiến độ: $completedCount/${tasks.size} công việc ($progressPercentage%)", 380f, 85f, paint)
            
            // Draw cards
            var currentY = 130f
            tasks.forEachIndexed { index, task ->
                if (currentY > 740f) {
                    // Simple page overflow message for simplicity or fits on page 1
                    paint.textSize = 11f
                    paint.color = android.graphics.Color.parseColor("#7A7A7A")
                    canvas.drawText("... và một số công việc khác được lọc bỏ ...", 40f, currentY + 5f, paint)
                    return@forEachIndexed
                } else {
                    val category = Category.getById(task.categoryId)
                    
                    // Card background (White surface)
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRoundRect(40f, currentY, 555f, currentY + 75f, 12f, 12f, paint)
                    
                    // Card border
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f
                    paint.color = android.graphics.Color.parseColor("#EAEAEA")
                    canvas.drawRoundRect(40f, currentY, 555f, currentY + 75f, 12f, 12f, paint)
                    
                    // Category vertical colored bar tag
                    // Convert Color to android.graphics.Color
                    paint.style = Paint.Style.FILL
                    val catColorHex = String.format("#%06X", 0xFFFFFF and category.color.toArgb())
                    paint.color = android.graphics.Color.parseColor(catColorHex)
                    canvas.drawRoundRect(46f, currentY + 8f, 52f, currentY + 67f, 3f, 3f, paint)
                    
                    // Icon and label
                    paint.textSize = 14f
                    canvas.drawText(category.iconEmoji, 64f, currentY + 28f, paint)
                    
                    // Task title
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.parseColor("#333333")
                    val truncatedTitle = if (task.title.length > 34) task.title.substring(0, 31) + "..." else task.title
                    canvas.drawText(truncatedTitle, 90f, currentY + 26f, paint)
                    
                    // Task desc
                    paint.textSize = 10f
                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.parseColor("#7A7A7A")
                    val descStr = if (task.description.isEmpty()) "Không có ghi chú thêm." else task.description
                    val truncatedDesc = if (descStr.length > 55) descStr.substring(0, 52) + "..." else descStr
                    canvas.drawText(truncatedDesc, 90f, currentY + 44f, paint)
                    
                    // Status Badge
                    val statusText = when (task.status) {
                        Task.STATUS_COMPLETED -> "HOÀN THÀNH"
                        Task.STATUS_MOVED -> "ĐÃ DỜI LỊCH"
                        else -> "CHỜ THỰC HIỆN"
                    }
                    val badgeBgColor = when (task.status) {
                        Task.STATUS_COMPLETED -> "#B5EAD7"
                        Task.STATUS_MOVED -> "#FFFFD1"
                        else -> "#EAEAEA"
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.parseColor(badgeBgColor)
                    canvas.drawRoundRect(90f, currentY + 52f, 180f, currentY + 67f, 6f, 6f, paint)
                    
                    paint.textSize = 8f
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.parseColor("#444444")
                    canvas.drawText(statusText, 102f, currentY + 63f, paint)
                    
                    // Display Scheduled Date and reminder time
                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.parseColor("#7A7A7A")
                    paint.textSize = 9f
                    canvas.drawText("Hạn chót: ${task.scheduledDate} lúc ${task.reminderTime}", 194f, currentY + 63f, paint)
                    
                    currentY += 88f
                }
            }
            
            // Footer decorative line and slogan
            paint.strokeWidth = 1f
            paint.color = android.graphics.Color.parseColor("#F5F5F5")
            canvas.drawLine(40f, 790f, 555f, 790f, paint)
            
            paint.textSize = 8.5f
            paint.color = android.graphics.Color.parseColor("#BDC3C7")
            canvas.drawText("Báo cáo được khởi tạo tự động bởi ứng dụng Lovely Scheduler. Chúc cậu một tuần mới đầy ngọt ngào! ❤️", 40f, 805f, paint)
            
            pdfDocument.finishPage(page)
            
            // Save to Downloads directory (safe fallback if permission issues occurs)
            val cacheDir = context.cacheDir
            val reportFile = File(cacheDir, "Lovely_Weekly_Report_${System.currentTimeMillis()}.pdf")
            
            val outputStream = FileOutputStream(reportFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            
            return reportFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
