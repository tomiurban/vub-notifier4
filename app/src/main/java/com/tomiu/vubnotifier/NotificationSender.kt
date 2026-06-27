package com.tomiu.vubnotifier

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Centrálna logika na odosielanie notifikácií na server.
 * Používa ju VubNotificationService (okamžité odoslanie), RetryWorker
 * (periodický pokus o znova-odoslanie) a MainActivity (manuálny retry).
 */
class NotificationSender(private val context: Context) {

    companion object {
        private const val TAG = "VubNotifier"
        const val PREFS = "vub_notifier_prefs"
        const val KEY_SERVER_URL = "server_url"
        const val DEFAULT_URL = "http://100.79.107.128:8899/api/vub-notification"
        private const val MAX_ATTEMPTS = 50 // ochrana pred nekonečným hromadením
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val queue = QueueStorage(context)

    private fun serverUrl(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    /** Pokus o synchrónne odoslanie. Vráti true ak server potvrdil prijatie (HTTP 2xx). */
    private fun trySend(title: String, text: String): Result<Unit> {
        return try {
            val json = JSONObject().apply {
                put("title", title)
                put("text", text)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(serverUrl()).post(body).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Nová notifikácia z VÚB appky. Najprv sa uloží do queue (aby sa nestratila
     * ak appka zhynie uprostred odosielania), potom sa skúsi okamžite odoslať.
     */
    fun enqueueAndSend(title: String, text: String) {
        val pending = PendingNotification(
            id = QueueStorage.newId(),
            title = title,
            text = text,
            receivedAt = System.currentTimeMillis()
        )
        queue.add(pending)
        Log.d(TAG, "Notifikácia uložená do queue: ${pending.id}")

        attemptSend(pending)
    }

    /** Pokus o odoslanie jednej položky z queue. Pri úspechu ju odstráni. */
    fun attemptSend(pending: PendingNotification) {
        val result = trySend(pending.title, pending.text)
        if (result.isSuccess) {
            queue.remove(pending.id)
            Log.d(TAG, "Úspešne odoslané a odstránené z queue: ${pending.id}")
        } else {
            val updated = pending.copy(
                attempts = pending.attempts + 1,
                lastError = result.exceptionOrNull()?.message ?: "neznáma chyba"
            )
            if (updated.attempts >= MAX_ATTEMPTS) {
                Log.e(TAG, "Notifikácia ${pending.id} prekročila max počet pokusov, mažem.")
                queue.remove(pending.id)
            } else {
                queue.update(updated)
                Log.e(TAG, "Odoslanie zlyhalo (pokus ${updated.attempts}): ${updated.lastError}")
            }
        }
    }

    /** Skúsi odoslať všetky čakajúce notifikácie v queue. Volá WorkManager aj manuálny retry. */
    fun retryAll(): Int {
        val pendingList = queue.getAll()
        pendingList.forEach { attemptSend(it) }
        return pendingList.size
    }

    fun pendingCount(): Int = queue.count()
    fun pendingList(): List<PendingNotification> = queue.getAll()
}
