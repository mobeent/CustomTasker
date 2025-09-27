package com.example.customtasker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.customtasker.model.Task

class TaskAdapter(private val taskList: List<Task>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val triggerTextView: TextView = itemView.findViewById(R.id.triggerTextView)
        val soundUriTextView: TextView = itemView.findViewById(R.id.soundUriTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.triggerTextView.text = "Trigger: ${task.triggerText}"
        holder.soundUriTextView.text = "Sound URI: ${task.soundUri}"
    }

    override fun getItemCount(): Int = taskList.size
}
