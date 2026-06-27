package com.tomiu.vubnotifier

import org.json.JSONObject

/**
 * Jedna čakajúca (nepotvrdená) VÚB notifikácia v queue.
 */
data class PendingNotification(
    val id: String,           // unikátne id (timestamp + random)
    val title: String,
    val text: String,
    val receivedAt: Long,     // kedy notifikácia prišla na telefón
    var attempts: Int = 0,    // počet pokusov o odoslanie
    var lastError: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("text", text)
        put("receivedAt", receivedAt)
        put("attempts", attempts)
        put("lastError", lastError ?: "")
    }

    companion object {
        fun fromJson(json: JSONObject): PendingNotification = PendingNotification(
            id = json.getString("id"),
            title = json.getString("title"),
            text = json.getString("text"),
            receivedAt = json.getLong("receivedAt"),
            attempts = json.optInt("attempts", 0),
            lastError = json.optString("lastError", "").ifEmpty { null }
        )
    }
}
