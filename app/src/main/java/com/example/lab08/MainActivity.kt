
package com.example.lab08

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        )
            .addMigrations(
                TaskDatabase.MIGRATION_1_2,
                TaskDatabase.MIGRATION_2_3,
                TaskDatabase.MIGRATION_3_4
            )
            .build()
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
                            return TaskViewModel(
                                application,
                                db.taskDao()
                            ) as T
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

    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()

    var newTask by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var selectedSort by remember { mutableStateOf("Fecha") }
    var selectedPriority by remember { mutableStateOf("Media") }
    var selectedCategory by remember { mutableStateOf("Personal") }
    var selectedRecurrence by remember { mutableStateOf("Ninguna") }
    var selectedReminder by remember { mutableLongStateOf(0L) }

    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editedDescription by remember { mutableStateOf("") }

    val blue = Color(0xFF2563EB)
    val background = Color(0xFFF3F6FC)

    val formatter = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun openReminderPicker() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val chosen = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (chosen.timeInMillis >
                            System.currentTimeMillis()
                        ) {
                            selectedReminder = chosen.timeInMillis

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU
                            ) {
                                permissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Selecciona una fecha y hora futura",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val filteredTasks = tasks
        .filter {
            it.description.contains(
                searchQuery,
                ignoreCase = true
            )
        }
        .filter {
            when (selectedFilter) {
                "Pendientes" -> !it.isCompleted
                "Completadas" -> it.isCompleted
                else -> true
            }
        }
        .let { list ->
            when (selectedSort) {
                "Nombre" -> list.sortedBy {
                    it.description.lowercase()
                }
                "Estado" -> list.sortedBy {
                    it.isCompleted
                }
                else -> list.sortedByDescending {
                    it.id
                }
            }
        }

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
                .padding(20.dp)
        ) {
            Text(
                "Mis Tareas",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Organiza tus actividades diarias",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar tareas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Todas",
                    "Pendientes",
                    "Completadas"
                ).forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(filter, fontSize = 11.sp)
                        }
                    )
                }
            }

            TaskDropdown(
                title = "Ordenar",
                selected = selectedSort,
                options = listOf(
                    "Fecha", "Nombre", "Estado"
                ),
                onSelected = { selectedSort = it }
            )

            HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newTask,
                onValueChange = { newTask = it },
                label = { Text("Nueva tarea") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TaskDropdown(
                        title = "Prioridad",
                        selected = selectedPriority,
                        options = listOf(
                            "Alta", "Media", "Baja"
                        ),
                        onSelected = { selectedPriority = it }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    TaskDropdown(
                        title = "Categoría",
                        selected = selectedCategory,
                        options = listOf(
                            "Estudios",
                            "Trabajo",
                            "Personal",
                            "Otros"
                        ),
                        onSelected = { selectedCategory = it }
                    )
                }
            }

            TaskDropdown(
                title = "Repetición",
                selected = selectedRecurrence,
                options = listOf(
                    "Ninguna",
                    "Diaria",
                    "Semanal",
                    "Mensual"
                ),
                onSelected = { selectedRecurrence = it }
            )

            OutlinedButton(
                onClick = { openReminderPicker() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedReminder == 0L)
                        "Elegir recordatorio"
                    else
                        "Avisar: ${formatter.format(Date(selectedReminder))}"
                )
            }

            if (selectedReminder > 0L) {
                TextButton(
                    onClick = { selectedReminder = 0L }
                ) {
                    Text("Quitar recordatorio")
                }
            }

            Button(
                onClick = {
                    if (newTask.isNotBlank()) {
                        if (
                            selectedReminder > 0L &&
                            selectedReminder <= System.currentTimeMillis()
                        ) {
                            Toast.makeText(
                                context,
                                "Elige un recordatorio futuro",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.addTask(
                                description = newTask,
                                priority = selectedPriority,
                                category = selectedCategory,
                                recurrence = selectedRecurrence,
                                reminderAt = selectedReminder
                            )

                            newTask = ""
                            selectedPriority = "Media"
                            selectedCategory = "Personal"
                            selectedRecurrence = "Ninguna"
                            selectedReminder = 0L
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Agregar tarea")
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Lista de tareas (${filteredTasks.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    filteredTasks,
                    key = { it.id }
                ) { task ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
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
                                    task.description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration =
                                    if (task.isCompleted)
                                        TextDecoration.LineThrough
                                    else
                                        TextDecoration.None
                                )

                                Text(
                                    "${task.category} | ${task.priority}",
                                    fontSize = 12.sp,
                                    color = when (task.priority) {
                                        "Alta" -> Color(0xFFDC2626)
                                        "Media" -> Color(0xFFD97706)
                                        else -> Color(0xFF16A34A)
                                    }
                                )

                                if (task.recurrence != "Ninguna") {
                                    Text(
                                        "Repetición: ${task.recurrence}",
                                        fontSize = 11.sp,
                                        color = blue
                                    )
                                }

                                if (task.dueDate > 0L) {
                                    Text(
                                        "Fecha: ${
                                            formatter.format(
                                                Date(task.dueDate)
                                            )
                                        }",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (task.reminderAt > 0L) {
                                    Text(
                                        "Recordatorio: ${
                                            formatter.format(
                                                Date(task.reminderAt)
                                            )
                                        }",
                                        fontSize = 11.sp,
                                        color = blue
                                    )
                                }

                                Text(
                                    if (task.isCompleted)
                                        "Completada"
                                    else
                                        "Pendiente",
                                    fontSize = 11.sp,
                                    color = Color.Gray
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
                                    contentDescription = "Editar",
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
                                    contentDescription = "Eliminar",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteAllTasks()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Eliminar todas las tareas",
                        color = Color.Red
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
                    onValueChange = {
                        editedDescription = it
                    },
                    label = { Text("Descripción") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingTask?.let {
                            viewModel.editTask(
                                it,
                                editedDescription
                            )
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

@Composable
fun TaskDropdown(
    title: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true }
        ) {
            Text(
                "$title: $selected",
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
