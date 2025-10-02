package com.example.customtasker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customtasker.databinding.ActivityAddTaskBinding
import com.example.customtasker.db.AppDatabase
import com.example.customtasker.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private lateinit var db: AppDatabase
    private var selectedSoundUri: Uri? = null
    private var isEditMode = false
    private var existingTaskId: Long? = null
    private var loadedTask: Task? = null

    companion object {
        const val REQUEST_CODE_PICK_SOUND = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 🧠 STEP 1: Check if we are editing
        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId != -1L) {
            isEditMode = true
            existingTaskId = taskId
            loadTaskForEditing(taskId)
        }

        binding.selectSoundButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_PICK_SOUND)
        }

        binding.saveTaskButton.setOnClickListener {
            val triggerText = binding.triggerEditText.text.toString()
            val soundUriString = selectedSoundUri?.toString()
                ?: loadedTask?.soundUri  // fallback to existing one
                ?: ""  // empty if nothing selected

            if (triggerText.isNotBlank() && soundUriString.isNotBlank()) {
                val task = Task(
                    id = existingTaskId ?: 0L,
                    triggerText = triggerText,
                    soundUri = soundUriString
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    if (isEditMode) {
                        db.taskDao().updateTask(task)
                    } else {
                        db.taskDao().insertTask(task)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddTaskActivity, "Task saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Trigger text cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTaskForEditing(taskId: Long) {
        Log.d("AddTaskActivity", "Loading task ID: $taskId")

        lifecycleScope.launch(Dispatchers.IO) {
            val task = db.taskDao().getTaskById(taskId)
            Log.d("AddTaskActivity", "Task loaded from DB: $task")
            if (task != null) {
                loadedTask = task
                withContext(Dispatchers.Main) {
                    binding.triggerEditText.setText(task.triggerText)
                    binding.soundUriText.text = task.soundUri
                    selectedSoundUri = Uri.parse(task.soundUri)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_SOUND && resultCode == Activity.RESULT_OK) {
            selectedSoundUri = data?.data
            if (selectedSoundUri == null) {
                Toast.makeText(this, "No audio file selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
