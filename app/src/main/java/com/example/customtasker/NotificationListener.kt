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
        val notificationText = extras.getCharSequence("android.text")?.toString()
        val packageName = sbn.packageName

        Log.d("NotifListener", "Received from $packageName: $notificationText")

        scope.launch {
            try {
                val tasks = taskDao.getAllTasks()
                for (task in tasks) {
                    if (notificationText?.contains(task.triggerText, ignoreCase = true) == true) {
                        Log.d("NotifListener", "Matched task: ${task.triggerText}")
                        withContext(Dispatchers.Main) {
                            SoundUtils.playSound(this@NotificationListener, task.soundUri.toUri())
                        }
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