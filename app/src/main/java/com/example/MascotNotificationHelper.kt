package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.model.MascotMessages

object MascotNotificationHelper {

    private const val CHANNEL_ID = "mascot_notifications"
    private const val CHANNEL_NAME = "Bò Béo Nhắc Nhở 🐮"
    private const val CHANNEL_DESC = "Nhận những lời nhắc nhở siêu cute từ linh vật Bò Béo"
    private const val NOTIFICATION_ID = 9912

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun triggerNotification(context: Context, status: String) {
        try {
            // Ensure channel is created
            createNotificationChannel(context)

            val message = MascotMessages.getNotificationContent(status)
            val title = when (status.uppercase()) {
                MascotMessages.STATE_HAPPY -> "Bò Béo đang reo hò nè! 🎉🥛"
                MascotMessages.STATE_REMIND -> "Đến giờ hẹn hò làm việc rùi cậu ơi! 🐮❤️"
                MascotMessages.STATE_SAD_PONTED -> "Hix... Bò Béo buồn thiu dỗi hờn... 🥺"
                MascotMessages.STATE_ANGRY_ABANDONED -> "CẢNH BÁO: Bò Béo đang bốc khói đầu! 🤬🔥"
                else -> "Lời nhắn nhủ từ Bò Béo! 🍼🥛"
            }

            // Tap notification to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // System safe dialog info drawable to avoid vector resource failures in OS notification tray
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)

            val notificationManager = NotificationManagerCompat.from(context)
            // Skip actual permission check for unit test/mocking, but catch any exception if API 33+ or custom OEMs are strict
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Throwable) {
            // Permission not granted, test environment, or unsupported resource/channel configuration
            e.printStackTrace()
        }
    }
}
