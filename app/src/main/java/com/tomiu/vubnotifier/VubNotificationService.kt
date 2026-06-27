package com.tomiu.vubnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class VubNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "VubNotifier"
        // OPRAVENÝ package name — overený priamo na zariadení cez Package Name Viewer.
        // Predošlá hodnota "sk.vub.mobilebanking" bola nesprávna, appka preto
        // tichoticho ignorovala všetky notifikácie.
        private const val VUB_PACKAGE = "sk.vub.banking"
    }

    private lateinit var sender: NotificationSender

    override fun onCreate() {
        super.onCreate()
        sender = NotificationSender(applicationContext)
        // Pri (re)štarte service skús hneď odoslať čo prípadne čaká v queue
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            RetryWorker.WORK_NAME, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RetryWorker>().build()
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != VUB_PACKAGE) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

        Log.d(TAG, "VÚB notifikácia: title=$title, text=$bigText")

        sender.enqueueAndSend(title, bigText)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Nič nerobíme pri odstránení notifikácie
    }
}
