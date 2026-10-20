@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.vanshmehta.prioritydo.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.data.model.Category
import com.example.vanshmehta.prioritydo.data.model.Importance
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Recurrence
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.ui.components.DateTimePickerDialog
import com.example.vanshmehta.prioritydo.util.DateFmt
import com.example.vanshmehta.prioritydo.util.TaskLogic

@Composable
fun TaskFormScreen(
    existing: Task?,
    saving: Boolean,
    onSave: (Task) -> Unit,
    onBack: () -> Unit
) {
    val key = existing?.id ?: "new"
    var title by rememberSaveable(key) { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable(key) { mutableStateOf(existing?.description ?: "") }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes ?: "") }
    var priority by rememberSaveable(key) { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var importance by rememberSaveable(key) { mutableStateOf(existing?.importance ?: Importance.NORMAL) }
    var category by rememberSaveable(key) { mutableStateOf(existing?.category ?: Category.OTHER) }
    var startAt by rememberSaveable(key) { mutableStateOf(existing?.startAt) }
    var dueAt by rememberSaveable(key) { mutableStateOf(existing?.dueAt) }
    var durationText by rememberSaveable(key) { mutableStateOf(existing?.durationMinutes?.toString() ?: "") }
    var recurrence by rememberSaveable(key) { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf("") } // "start" | "due" | ""

    val durationMin = durationText.trim().toIntOrNull()
    val rangeError = startAt != null && dueAt != null && dueAt!! < startAt!!

    fun build(): Task? {
        if (durationText.isNotBlank() && durationMin == null) { error = "Duration must be a whole number of minutes"; return null }
        val msg = TaskLogic.validate(title, startAt, dueAt, durationMin, recurrence)
        if (msg != null) { error = msg; return null }
        error = null
        return (existing ?: Task()).copy(
            title = title.trim(), description = description.trim(), notes = notes.trim(),
            priority = priority, importance = importance, category = category,
            startAt = startAt, dueAt = dueAt, durationMinutes = durationMin, recurrence = recurrence
        )
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(if (existing == null) "Add Task" else "Edit Task", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(100); error = null }, label = { Text("Task title *") }, singleLine = true,
                isError = error?.contains("Title") == true,
                supportingText = { Text(if (error?.contains("Title") == true) error!! else "${title.length}/100") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it.take(2000) }, label = { Text("Description (optional)") },
                minLines = 2, modifier = Modifier.fillMaxWidth()
            )

            Label("Priority *")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.label) }) }
            }
            Label("Importance *")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Importance.values().forEach { i -> FilterChip(importance == i, { importance = i }, { Text(i.label) }) }
            }
            Label("Category *")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.values().forEach { c -> FilterChip(category == c, { category = c }, { Text(c.label) }) }
            }

            HorizontalDivider()
            Label("Schedule (optional)")
            DateRow("Start date and time", startAt, { picker = "start" }, { startAt = null; error = null })
            DateRow("Deadline", dueAt, { picker = "due" }, { dueAt = null; error = null })
            if (rangeError) {
                Text("Deadline cannot be earlier than the start time", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = durationText, onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(4); error = null },
                label = { Text("Estimated duration (minutes)") }, singleLine = true,
                isError = error?.contains("Duration") == true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("1 to ${TaskLogic.MAX_DURATION_MIN} minutes. Leave empty if unknown.") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Label("Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.values().forEach { r -> FilterChip(recurrence == r, { recurrence = r; error = null }, { Text(r.label) }) }
            }
            if (recurrence != Recurrence.NONE) {
                Text(
                    "When you complete this task, the next occurrence is created once, based on the deadline (or start time). Reopening a completed task never creates another copy.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = notes, onValueChange = { notes = it.take(2000) }, label = { Text("Notes (optional)") },
                minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            if (error != null && error?.contains("Title") != true) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = { build()?.let(onSave) },
                enabled = !saving && !rangeError,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (existing == null) "Add task" else "Save changes")
            }
            Row(Modifier.height(16.dp)) {}
        }
    }

    when (picker) {
        "start" -> DateTimePickerDialog(startAt, "Start", { picker = "" }) { startAt = it; picker = ""; error = null }
        "due" -> DateTimePickerDialog(dueAt, "Deadline", { picker = "" }) { dueAt = it; picker = ""; error = null }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun DateRow(label: String, value: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onPick)) {
        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, contentDescription = null)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value?.let { DateFmt.dateTime(it) } ?: "Not set", style = MaterialTheme.typography.bodyLarge)
            }
            if (value != null) IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = "Clear $label") }
        }
    }
}
