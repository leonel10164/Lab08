
package com.example.lab08

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val description: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(defaultValue = "'Media'")
    val priority: String = "Media",

    @ColumnInfo(defaultValue = "'Personal'")
    val category: String = "Personal",

    @ColumnInfo(defaultValue = "'Ninguna'")
    val recurrence: String = "Ninguna",

    @ColumnInfo(defaultValue = "0")
    val dueDate: Long = 0L,

    @ColumnInfo(defaultValue = "0")
    val reminderAt: Long = 0L
)
