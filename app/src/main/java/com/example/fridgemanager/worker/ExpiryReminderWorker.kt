package com.example.fridgemanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.fridgemanager.MainActivity
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.fridgemanager.R
import com.example.fridgemanager.data.preferences.UserPreferences
import com.example.fridgemanager.data.repository.FoodRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpiryReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FoodRepository,
    private val prefs: UserPreferences
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "expiry_reminder_daily"
        const val CHANNEL_ID = "expiry_reminder"
        const val NOTIFICATION_ID = 1001

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(Constraints.NONE)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** 计算到明天上午 9 点的延迟（毫秒） */
        private fun calculateInitialDelay(): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0)
        }
    }

    override suspend fun doWork(): Result {
        val settings = prefs.reminderSettings.first()
        if (!settings.enabled) return Result.success()

        val expiringItems = repository.getExpiringItems(settings.daysBefore).first()
        if (expiringItems.isEmpty()) return Result.success()

        createNotificationChannel()

        fun daysText(days: Long?) = when {
            days == null -> "未知"
            days < 0    -> "已过期"
            days == 0L  -> "今天到期"
            else        -> "还剩 ${days} 天"
        }

        val message = when {
            expiringItems.size == 1 ->
                "「${expiringItems[0].name}」${daysText(expiringItems[0].daysUntilExpiry)}"
            else ->
                "有 ${expiringItems.size} 件食材即将到期，请尽快食用"
        }

        val bigText = expiringItems.joinToString("\n") { item ->
            "· ${item.name}  ${daysText(item.daysUntilExpiry)}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("冰箱管家 · 临期提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 使用固定 ID 使每日提醒覆盖上一条，而不是堆积多条
        nm.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "临期食材提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "提醒您冰箱中即将到期的食材"
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
