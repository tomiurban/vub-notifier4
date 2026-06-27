package com.tomiu.vubnotifier

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tomiu.vubnotifier.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sender: NotificationSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sender = NotificationSender(applicationContext)
        BootReceiver.schedulePeriodicRetry(applicationContext)

        val prefs = getSharedPreferences(NotificationSender.PREFS, MODE_PRIVATE)
        binding.etServerUrl.setText(
            prefs.getString(NotificationSender.KEY_SERVER_URL, NotificationSender.DEFAULT_URL)
        )

        binding.btnSave.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Zadaj URL servera", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString(NotificationSender.KEY_SERVER_URL, url).apply()
            Toast.makeText(this, "URL uložená ✓", Toast.LENGTH_SHORT).show()
        }

        binding.btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnRetryNow.setOnClickListener {
            Toast.makeText(this, "Odosielam čakajúce notifikácie...", Toast.LENGTH_SHORT).show()
            Thread {
                val count = sender.retryAll()
                runOnUiThread {
                    Toast.makeText(this, "Hotovo ($count pokusov)", Toast.LENGTH_SHORT).show()
                    refreshQueueView()
                }
            }.start()
        }

        updateStatus()
        refreshQueueView()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshQueueView()
    }

    private fun updateStatus() {
        val enabled = isNotificationListenerEnabled()
        if (enabled) {
            binding.tvStatus.text = "✅ Povolenie aktívne — VÚB notifikácie sa posielajú"
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.btnPermission.text = "Skontrolovať povolenie"
        } else {
            binding.tvStatus.text = "⚠️ Povolenie nie je udelené — klikni na tlačidlo nižšie"
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            binding.btnPermission.text = "Udeliť povolenie"
        }
    }

    private fun refreshQueueView() {
        val pending = sender.pendingList()
        binding.queueContainer.removeAllViews()

        if (pending.isEmpty()) {
            binding.tvQueueTitle.text = "ČAKAJÚCE NOTIFIKÁCIE (0)"
            binding.btnRetryNow.visibility = View.GONE
            val emptyView = TextView(this).apply {
                text = "Žiadne čakajúce notifikácie — všetko odoslané ✓"
                setTextColor(getColor(android.R.color.darker_gray))
                textSize = 13f
                setPadding(0, 8, 0, 0)
            }
            binding.queueContainer.addView(emptyView)
            return
        }

        binding.tvQueueTitle.text = "ČAKAJÚCE NOTIFIKÁCIE (${pending.size})"
        binding.btnRetryNow.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        pending.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(0xFF16252E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }
            val titleView = TextView(this).apply {
                text = item.title
                setTextColor(getColor(android.R.color.white))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val metaView = TextView(this).apply {
                val time = sdf.format(Date(item.receivedAt))
                text = "Prijaté $time · pokusov: ${item.attempts}" +
                    (item.lastError?.let { " · $it" } ?: "")
                setTextColor(0xFFFF6B5E.toInt())
                textSize = 11f
                setPadding(0, 4, 0, 0)
            }
            row.addView(titleView)
            row.addView(metaView)
            binding.queueContainer.addView(row)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val cn = ComponentName(this, VubNotificationService::class.java)
        return flat.contains(cn.flattenToString())
    }
}
