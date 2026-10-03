package com.sync.whatsapponedrive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnableNotifications: Button
    private lateinit var btnGrantStorage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnEnableNotifications = findViewById(R.id.btnEnableNotifications)
        btnGrantStorage = findViewById(R.id.btnGrantStorage)

        btnEnableNotifications.setOnClickListener {
            // Open Android Notification Access Settings for WhatsApp Reader
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable WhatsApp to OneDrive in the list", Toast.LENGTH_LONG).show()
        }

        btnGrantStorage.setOnClickListener {
            requestStoragePermissions()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val isNotificationEnabled = isNotificationServiceEnabled()
        val prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val lastCustomer = prefs.getString("last_customer_name", "None")

        if (isNotificationEnabled) {
            tvStatus.text = "🟢 Active & Running\nListening for WhatsApp Photos\nLast Detected Customer: $lastCustomer"
        } else {
            tvStatus.text = "🔴 Setup Required: Please grant Notification Access below"
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun requestStoragePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
        } else {
            Toast.makeText(this, "Storage permission already granted!", Toast.LENGTH_SHORT).show()
        }
    }
}
