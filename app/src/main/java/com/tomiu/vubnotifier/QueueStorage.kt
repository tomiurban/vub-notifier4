package com.tomiu.vubnotifier

import android.content.Context
import org.json.JSONArray
import java.util.UUID

/**
 * Jednoduché perzistentné úložisko pre čakajúce notifikácie.
 * Používa SharedPreferences (JSON array) — pre tento účel postačuje,
 * žiadna potreba plnej SQLite/Room databázy.
 */
class QueueStorage(context: Context) {

    private val prefs = context.getSharedPreferences("vub_notifier_queue", Context.MODE_PRIVATE)
    private val KEY_QUEUE = "pending_queue"

    @Synchronized
    fun getAll(): List<PendingNotification> {
        val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<PendingNotification>()
        for (i in 0 until arr.length()) {
            try {
                list.add(PendingNotification.fromJson(arr.getJSONObject(i)))
            } catch (_: Exception) { /* preskočiť poškodený záznam */ }
        }
        return list.sortedBy { it.receivedAt }
    }

    @Synchronized
    fun add(notification: PendingNotification) {
        val current = getAll().toMutableList()
        current.add(notification)
        saveAll(current)
    }

    @Synchronized
    fun remove(id: String) {
        val current = getAll().filter { it.id != id }
        saveAll(current)
    }

    @Synchronized
    fun update(notification: PendingNotification) {
        val current = getAll().map { if (it.id == notification.id) notification else it }
        saveAll(current)
    }

    @Synchronized
    fun count(): Int = getAll().size

    private fun saveAll(list: List<PendingNotification>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    companion object {
        fun newId(): String = "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
    }
}
