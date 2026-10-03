package com.sync.whatsapponedrive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createServiceNotification("Auto-syncing WhatsApp photos...")
        startForeground(1001, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val customerName = intent?.getStringExtra("customer_name") ?: "Customer"

        serviceScope.launch {
            // Wait 2-3 seconds for WhatsApp to finish downloading the image
            delay(3000)

            processLatestPhotos(customerName)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun processLatestPhotos(customerName: String) {
        val prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("onedrive_token", null) ?: return
        val lastProcessedTime = prefs.getLong("last_processed_timestamp", 0)

        val possiblePaths = listOf(
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images"),
            File(Environment.getExternalStorageDirectory(), "Pictures/WhatsApp")
        )

        for (dir in possiblePaths) {
            if (dir.exists() && dir.isDirectory) {
                val newFiles = dir.listFiles { file ->
                    file.isFile &&
                    (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true)) &&
                    file.lastModified() > lastProcessedTime
                }?.sortedBy { it.lastModified() } ?: emptyList()

                val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

                for (file in newFiles) {
                    val dateStr = timeFormat.format(Date(file.lastModified()))
                    val sanitizedCustomer = customerName.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "").trim()
                    val newFileName = "${sanitizedCustomer}_${dateStr}.${file.extension}"

                    // Upload directly to OneDrive under Customer's Dedicated Folder
                    val success = OneDriveUploader.uploadFile(
                        accessToken = token,
                        folderName = sanitizedCustomer,
                        fileName = newFileName,
                        file = file
                    )

                    if (success) {
                        prefs.edit().putLong("last_processed_timestamp", file.lastModified()).apply()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sync_channel",
                "WhatsApp Auto Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "sync_channel")
            .setContentTitle("WhatsApp to OneDrive Sync")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
