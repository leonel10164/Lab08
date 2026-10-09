
package com.example.lab08

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun schedule(context: Context, task: Task) {

        if (task.reminderAt <= 0L || task.isCompleted) {
            return
        }

        val delay = task.reminderAt -
                System.currentTimeMillis()

        if (delay <= 0L) return

        val data = Data.Builder()
            .putInt("taskId", task.id)
            .putString("description", task.description)
            .build()

        val request =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .setInputData(data)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "task_reminder_${task.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancel(context: Context, taskId: Int) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("task_reminder_$taskId")
    }
}
