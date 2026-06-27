package com.tomiu.vubnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("VubNotifier", "Boot completed - naplánovaný periodický retry")
            schedulePeriodicRetry(context)
        }
    }

    companion object {
        /** Naplánuje periodickú kontrolu queue každých 15 minút (minimálny interval WorkManagera). */
        fun schedulePeriodicRetry(context: Context) {
            val request = PeriodicWorkRequestBuilder<RetryWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                "${RetryWorker.WORK_NAME}_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
