package com.example.fridgemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.example.fridgemanager.worker.ExpiryReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ExpiryReminderWorker.schedule(WorkManager.getInstance(context))
        }
    }
}
