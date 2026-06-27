package com.tomiu.vubnotifier

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodicky (a pri obnovení siete) skúša odoslať všetky notifikácie
 * čakajúce v lokálnej queue. Bežný "store-and-forward" vzor — žiadna
 * notifikácia sa nestratí len pre to, že v momente príchodu nebola sieť.
 */
class RetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "VubNotifier"
        const val WORK_NAME = "vub_retry_worker"
    }

    override suspend fun doWork(): Result {
        val sender = NotificationSender(applicationContext)
        val pendingBefore = sender.pendingCount()

        if (pendingBefore == 0) {
            return Result.success()
        }

        Log.d(TAG, "RetryWorker: skúšam odoslať $pendingBefore čakajúcich notifikácií")
        sender.retryAll()

        val pendingAfter = sender.pendingCount()
        Log.d(TAG, "RetryWorker: po pokuse zostáva $pendingAfter čakajúcich")

        // Vráť success aj keď niečo zostalo v queue — periodická úloha
        // sa o to postará znova nabudúce. "Retry" v zmysle WorkManagera
        // by zbytočne navyšoval backoff delay.
        return Result.success()
    }
}
