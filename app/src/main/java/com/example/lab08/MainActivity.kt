
package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Lab08Theme {
                val taskViewModel: TaskViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return TaskViewModel(db.taskDao()) as T
                        }
                    }
                )

                TaskScreen(taskViewModel)
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {

    val tasks by viewModel.tasks.collectAsState()
    var newTask by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editedDescription by remember { mutableStateOf("") }

    val blue = Color(0xFF2563EB)
    val background = Color(0xFFF3F6FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(blue)
                .padding(24.dp)
        ) {
            Text(
                text = "Mis Tareas",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Organiza tus actividades diarias",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = newTask,
                onValueChange = { newTask = it },
                label = { Text("Escribe una nueva tarea") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (newTask.isNotBlank()) {
                        viewModel.addTask(newTask)
                        newTask = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar tarea")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Lista de tareas (${tasks.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = {
                                    viewModel.toggleTaskCompletion(task)
                                }
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = task.description,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration =
                                    if (task.isCompleted)
                                        TextDecoration.LineThrough
                                    else
                                        TextDecoration.None
                                )

                                Text(
                                    text = if (task.isCompleted)
                                        "Completada"
                                    else
                                        "Pendiente",
                                    fontSize = 12.sp,
                                    color = if (task.isCompleted)
                                        Color(0xFF16A34A)
                                    else
                                        Color.Gray
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingTask = task
                                    editedDescription = task.description
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar tarea",
                                    tint = blue
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteTask(task)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar tarea",
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.deleteAllTasks() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Eliminar todas las tareas",
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }

    if (editingTask != null) {
        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("Editar tarea") },
            text = {
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Descripción") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingTask?.let {
                            viewModel.editTask(it, editedDescription)
                        }
                        editingTask = null
                    },
                    enabled = editedDescription.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingTask = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
