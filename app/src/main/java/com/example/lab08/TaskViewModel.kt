
package com.example.lab08

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(
    application: Application,
    private val taskDao: TaskDao
) : AndroidViewModel(application) {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val context = application.applicationContext

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskDao.getAllTasks()
        }
    }

    fun addTask(
        description: String,
        priority: String = "Media",
        category: String = "Personal",
        recurrence: String = "Ninguna",
        reminderAt: Long = 0L
    ) {
        if (description.isBlank()) return

        viewModelScope.launch {
            val task = Task(
                description = description.trim(),
                priority = priority,
                category = category,
                recurrence = recurrence,
                dueDate = System.currentTimeMillis(),
                reminderAt = reminderAt
            )

            val newId = taskDao.insertTask(task).toInt()

            ReminderScheduler.schedule(
                context,
                task.copy(id = newId)
            )

            loadTasks()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val completed = !task.isCompleted

            taskDao.updateTask(
                task.copy(isCompleted = completed)
            )

            if (completed) {
                ReminderScheduler.cancel(context, task.id)

                if (task.recurrence != "Ninguna") {
                    val calendar = Calendar.getInstance()

                    calendar.timeInMillis =
                        if (task.dueDate > 0L)
                            task.dueDate
                        else
                            System.currentTimeMillis()

                    when (task.recurrence) {
                        "Diaria" ->
                            calendar.add(Calendar.DAY_OF_YEAR, 1)

                        "Semanal" ->
                            calendar.add(Calendar.WEEK_OF_YEAR, 1)

                        "Mensual" ->
                            calendar.add(Calendar.MONTH, 1)
                    }

                    val nextReminder = if (task.reminderAt > 0L) {
                        val reminderCalendar = Calendar.getInstance()
                        reminderCalendar.timeInMillis = task.reminderAt

                        when (task.recurrence) {
                            "Diaria" ->
                                reminderCalendar.add(
                                    Calendar.DAY_OF_YEAR, 1
                                )

                            "Semanal" ->
                                reminderCalendar.add(
                                    Calendar.WEEK_OF_YEAR, 1
                                )

                            "Mensual" ->
                                reminderCalendar.add(
                                    Calendar.MONTH, 1
                                )
                        }

                        reminderCalendar.timeInMillis
                    } else {
                        0L
                    }

                    val nextTask = task.copy(
                        id = 0,
                        isCompleted = false,
                        dueDate = calendar.timeInMillis,
                        reminderAt = nextReminder
                    )

                    val nextId = taskDao.insertTask(nextTask).toInt()

                    ReminderScheduler.schedule(
                        context,
                        nextTask.copy(id = nextId)
                    )
                }
            } else {
                ReminderScheduler.schedule(
                    context,
                    task.copy(isCompleted = false)
                )
            }

            loadTasks()
        }
    }

    fun editTask(task: Task, newDescription: String) {
        if (newDescription.isBlank()) return

        viewModelScope.launch {
            val updatedTask = task.copy(
                description = newDescription.trim()
            )

            taskDao.updateTask(updatedTask)

            ReminderScheduler.schedule(
                context,
                updatedTask
            )

            loadTasks()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            ReminderScheduler.cancel(context, task.id)
            taskDao.deleteTask(task)
            loadTasks()
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val currentTasks = taskDao.getAllTasks()

            currentTasks.forEach { task ->
                ReminderScheduler.cancel(context, task.id)
            }

            taskDao.deleteAllTasks()
            loadTasks()
        }
    }
}
