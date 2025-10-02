package com.example.customtasker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.customtasker.model.Task

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onDeleteClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val triggerTextView: TextView = itemView.findViewById(R.id.triggerTextView)
        val soundUriTextView: TextView = itemView.findViewById(R.id.soundUriTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
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

        holder.deleteButton.setOnClickListener {
            onDeleteClick(task)
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun removeTask(task: Task) {
        val index = taskList.indexOf(task)
        if (index != -1) {
            taskList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}