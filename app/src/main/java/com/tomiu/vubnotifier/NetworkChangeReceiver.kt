package com.tomiu.vubnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Počúva na zmenu sieťového pripojenia. Keď sa sieť obnoví, okamžite
 * spustí RetryWorker namiesto čakania na ďalší periodický cyklus —
 * notifikácie sa tak odošlú v priebehu sekúnd po návrate signálu,
 * nie až o niekoľko minút neskôr.
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VubNotifier"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetworkInfo
        val isConnected = activeNetwork?.isConnected == true

        if (isConnected) {
            Log.d(TAG, "Sieť obnovená — spúšťam okamžitý retry")
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                RetryWorker.WORK_NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RetryWorker>().build()
            )
        }
    }
}
