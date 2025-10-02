package com.example.customtasker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
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
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
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
            val uri = data?.data
            if (uri != null) {
                // Persist access permission
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedSoundUri = copyAudioToMediaStore(uri) ?: uri
                binding.soundUriText.text = selectedSoundUri.toString()
            } else {
                Toast.makeText(this, "No audio file selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyAudioToMediaStore(sourceUri: Uri): Uri? {
        val resolver = contentResolver
        val fileName = queryFileName(sourceUri) ?: "audio_${System.currentTimeMillis()}.mp3"

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/MyAppAudios")
            put(MediaStore.Audio.Media.IS_MUSIC, true)
        }

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val newAudioUri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(newAudioUri).use { output ->
            resolver.openInputStream(sourceUri).use { input ->
                input?.copyTo(output!!)
            }
        }

        return newAudioUri
    }

    private fun queryFileName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
