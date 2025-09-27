package com.example.customtasker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    companion object {
        const val REQUEST_CODE_PICK_SOUND = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        binding.selectSoundButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_SOUND)
        }

        binding.saveTaskButton.setOnClickListener {
            val triggerText = binding.triggerEditText.text.toString()
            selectedSoundUri?.let { uri ->
                val task = Task(triggerText = triggerText, soundUri = uri.toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    db.taskDao().insertTask(task)
                    withContext(Dispatchers.Main) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_SOUND && resultCode == Activity.RESULT_OK) {
            selectedSoundUri = data?.data
            binding.soundUriText.text = selectedSoundUri.toString()
        }
    }
}