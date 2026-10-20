@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.vanshmehta.prioritydo.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.ui.components.ProgressRing
import com.example.vanshmehta.prioritydo.ui.components.successColor
import com.example.vanshmehta.prioritydo.util.DateFmt
import com.example.vanshmehta.prioritydo.viewmodel.TimerState

@Composable
fun TimerScreen(
    state: TimerState,
    tasks: List<Task>,
    presetTask: Task?,
    onBack: () -> Unit,
    onConfigure: (Long) -> Unit,
    onLink: (Task?) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    requestNotificationPermission: () -> Unit
) {
    // Opened from Task Details: use the task's estimated duration when it has one.
    LaunchedEffect(presetTask?.id) {
        if (presetTask != null && !state.running && state.taskId != presetTask.id) {
            presetTask.durationMinutes?.let { onConfigure(it * 60_000L) }
            onLink(presetTask)
        }
    }
    var customMin by rememberSaveable { mutableStateOf("") }
    var linkDialog by rememberSaveable { mutableStateOf(false) }
    val progress = if (state.totalMs == 0L) 0f else state.remainingMs.toFloat() / state.totalMs
    val paused = !state.running && state.remainingMs in 1 until state.totalMs

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Focus Timer", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressRing(
                fraction = if (state.finished) 1f else progress, size = 260.dp, strokeWidth = 14.dp,
                color = if (state.finished) successColor() else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.finished) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = successColor())
                    }
                    Text(DateFmt.mmss(state.remainingMs), style = MaterialTheme.typography.displayMedium)
                    Text(
                        when { state.finished -> "Session complete"; state.running -> "Running"; paused -> "Paused"; else -> "Ready" },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                state.taskTitle?.let { "Linked task: $it" } ?: "Not linked to a task",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.running) {
                    Button(onClick = onPause) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause") }
                } else {
                    Button(
                        onClick = { requestNotificationPermission(); onStart() },
                        enabled = state.remainingMs > 0
                    ) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (paused) "Resume" else "Start")
                    }
                }
                OutlinedButton(onClick = onReset) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") }
            }
            if (!state.running) {
                Text("Duration", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(5, 10, 15, 25, 45, 60).forEach { m ->
                        FilterChip(state.totalMs == m * 60_000L, { onConfigure(m * 60_000L) }, { Text("$m min") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customMin, onValueChange = { customMin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Custom minutes (1-1440)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { customMin.toLongOrNull()?.takeIf { it in 1..1440 }?.let { onConfigure(it * 60_000L) } }) { Text("Set") }
                }
                OutlinedButton(onClick = { linkDialog = true }) { Text(if (state.taskId == null) "Link a task (optional)" else "Change linked task") }
                if (state.taskId != null) TextButton(onClick = { onLink(null) }) { Text("Unlink task") }
            }
            Text(
                "The focus timer is independent from task deadlines. When a session you started finishes, a notification may be shown if you allow notifications.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (linkDialog) {
        val active = tasks.filter { !it.isCompleted }
        AlertDialog(
            onDismissRequest = { linkDialog = false },
            title = { Text("Link a task") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (active.isEmpty()) Text("No active tasks.")
                    active.forEach { t -> TextButton(onClick = { onLink(t); linkDialog = false }) { Text(t.title) } }
                }
            },
            confirmButton = { TextButton(onClick = { linkDialog = false }) { Text("Close") } }
        )
    }
}
