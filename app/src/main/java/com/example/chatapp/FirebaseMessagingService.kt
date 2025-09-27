package com.example.chatapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class FirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FirebaseMessageService", "From: ${message.from} Data: ${message.notification}")
        message.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        // Vibration pattern
        val vibrationPattern = longArrayOf(0, 100, 200, 100)

        // Avoid showing notification if it contains the current user's display name
        Firebase.auth.currentUser?.let {
            if (title?.contains(it.displayName.toString()) == true || message?.contains(it.displayName.toString()) == true) return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure the notification channel is created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "messages"
            val channelName = "Messages"
            val descriptionText = "Notifications for the Messages"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // Check if channel already exists
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    this.vibrationPattern = vibrationPattern
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Create the notification
        val notificationId = Random().nextInt(1000)
        val notificationBuilder = NotificationCompat.Builder(this, "messages")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVibrate(if (isVibrationEnabled()) vibrationPattern else null)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // This is mainly for pre-Oreo devices
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    // Method to check if vibration is enabled on the device
    private fun isVibrationEnabled(): Boolean {
        val audioManager = ContextCompat.getSystemService(this, android.media.AudioManager::class.java)
        return audioManager?.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL
    }
}
