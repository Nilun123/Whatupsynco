package com.sync.whatsapponedrive

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return
        val packageName = sbn.packageName

        // Listen to standard WhatsApp and WhatsApp Business
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: "" // Sender / Customer Name
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.d("WhatsAppListener", "Received notification from: $title | Text: $text")

            // Check if notification indicates a photo / image
            val isPhoto = text.contains("Photo", ignoreCase = true) ||
                          text.contains("📷") ||
                          text.contains("image", ignoreCase = true) ||
                          text.contains("picture", ignoreCase = true)

            if (title.isNotEmpty()) {
                val prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("last_customer_name", title.trim())
                    putLong("last_notification_time", System.currentTimeMillis())
                    apply()
                }

                // Trigger Background Media Check & Upload
                val syncIntent = Intent(this, MediaSyncService::class.java).apply {
                    putExtra("customer_name", title.trim())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(syncIntent)
                } else {
                    startService(syncIntent)
                }
            }
        }
    }
}
