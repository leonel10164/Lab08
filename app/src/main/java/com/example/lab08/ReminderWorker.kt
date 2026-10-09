
package com.example.lab08

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val taskId = inputData.getInt("taskId", -1)

        val description = inputData.getString("description")
            ?: return Result.failure()

        if (taskId < 0) {
            return Result.failure()
        }

        val channelId = "task_reminders"

        val notificationManager =
            applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Recordatorios de tareas",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description =
                "Notificaciones de tareas pendientes"

            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permission = applicationContext.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permission != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de tarea")
            .setContentText(description)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Tienes una tarea pendiente: $description"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(taskId, notification)

        return Result.success()
    }
}
