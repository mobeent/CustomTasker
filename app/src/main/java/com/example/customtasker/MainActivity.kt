package com.example.customtasker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.customtasker.databinding.ActivityMainBinding
import com.example.customtasker.db.AppDatabase
import com.example.customtasker.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private var taskList = mutableListOf<Task>()

    companion object {
        private const val CHANNEL_ID = "test_channel"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(
            taskList,
            onDeleteClick = { taskToDelete ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.taskDao().deleteTask(taskToDelete)
                    }
                    adapter.removeTask(taskToDelete)
                }
            },
            onItemClick = { selectedTask ->
                Log.d("MainActivity", "Opening task with ID: ${selectedTask.id}")

                val intent = Intent(this, AddTaskActivity::class.java)
                intent.putExtra("task_id", selectedTask.id)
                startActivity(intent)
            }
        )
        binding.taskRecyclerView.adapter = adapter

        binding.addTaskButton.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        // Load tasks from DB asynchronously
        lifecycleScope.launch {
            val tasksFromDb = withContext(Dispatchers.IO) {
                db.taskDao().getAllTasks()
            }

            Log.d("MainActivity", "Fetched ${tasksFromDb.size} tasks from DB")
            tasksFromDb.forEach { Log.d("MainActivity", "Task: id=${it.id}, trigger=${it.triggerText}") }

            taskList.clear()
            taskList.addAll(tasksFromDb)
            adapter.notifyDataSetChanged()
        }

        binding.sendTestNotificationButton.setOnClickListener {
            sendTestNotification()
        }

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        // Reload tasks when returning from AddTaskActivity
        lifecycleScope.launch {
            val tasksFromDb = withContext(Dispatchers.IO) {
                db.taskDao().getAllTasks()
            }

            Log.d("MainActivity", "Fetched ${tasksFromDb.size} tasks from DB")
            tasksFromDb.forEach { Log.d("MainActivity", "Task: id=${it.id}, trigger=${it.triggerText}") }

            taskList.clear()
            taskList.addAll(tasksFromDb)
            adapter.notifyDataSetChanged()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Test Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for test notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
                return
            }
        }
        actuallySendNotification()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun actuallySendNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Replace with your icon
            .setContentTitle("Find my phone")
            .setContentText("Find my phone")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(1001, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                actuallySendNotification()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}