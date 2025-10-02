package com.example.customtasker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.*
import com.example.customtasker.db.AppDatabase
import com.example.customtasker.model.TaskDao

class NotificationListener : NotificationListenerService() {

    private lateinit var taskDao: TaskDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim().orEmpty()

        val fullText = "$title\n$text\n$bigText".lowercase()
        val packageName = sbn.packageName

        if (fullText.isNullOrBlank()) {
            Log.d("NotifListener", "Notification from $packageName has no text. Ignored.")
            return
        }

        Log.d("NotifListener", "Received from $packageName: $fullText")

        scope.launch {
            try {
                val tasks = taskDao.getAllTasks()
                for (task in tasks) {
                    // Skip blank triggers
                    if (task.triggerText.isBlank()) continue

                    // Normalize strings
                    val trigger = task.triggerText.trim()
                    val notifText = fullText.trim()

                    if (notifText.contains(trigger, ignoreCase = true)) {
                        Log.d("NotifListener", "✅ Matched task: '${task.triggerText}'")
                        withContext(Dispatchers.Main) {
                            SoundUtils.playSound(this@NotificationListener, Uri.parse(task.soundUri))
                        }
                        break
                    } else {
                        Log.d("NotifListener", "❌ No match for: '${task.triggerText}'")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotifListener", "Error reading tasks from DB: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}