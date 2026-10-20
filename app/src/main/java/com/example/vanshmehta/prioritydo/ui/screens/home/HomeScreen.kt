@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.vanshmehta.prioritydo.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.data.model.UserProfile
import com.example.vanshmehta.prioritydo.ui.components.EmptyState
import com.example.vanshmehta.prioritydo.ui.components.ErrorState
import com.example.vanshmehta.prioritydo.ui.components.LocalReduceMotion
import com.example.vanshmehta.prioritydo.ui.components.OfflineBanner
import com.example.vanshmehta.prioritydo.ui.components.ProgressRing
import com.example.vanshmehta.prioritydo.ui.components.SectionHeader
import com.example.vanshmehta.prioritydo.ui.components.SkeletonList
import com.example.vanshmehta.prioritydo.ui.components.TaskActions
import com.example.vanshmehta.prioritydo.ui.components.TaskCard
import com.example.vanshmehta.prioritydo.ui.components.priorityColor
import com.example.vanshmehta.prioritydo.ui.components.successColor
import com.example.vanshmehta.prioritydo.ui.theme.Sun
import com.example.vanshmehta.prioritydo.util.DateFmt
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.example.vanshmehta.prioritydo.viewmodel.TaskUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun HomeScreen(
    profile: UserProfile?,
    ui: TaskUiState,
    actions: TaskActions,
    onOpenFilter: (String) -> Unit,
    onOpenInsights: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenProfile: () -> Unit,
    onQuickAdd: (String, Priority) -> Unit,
    onAddTask: () -> Unit
) {
    var showQuick by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Home", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { showQuick = true }) { Icon(Icons.Default.Add, contentDescription = "Quick add task") }
                IconButton(onClick = onOpenProfile) {
                    Box(Modifier.size(32.dp).background(Sun.Golden, CircleShape), contentAlignment = Alignment.Center) {
                        Text(
                            (profile?.displayName?.take(1) ?: "?").uppercase(),
                            color = Sun.Navy, style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        )
        if (!ui.online) OfflineBanner()
        when {
            ui.loading -> SkeletonList()
            ui.error != null -> ErrorState(ui.error)
            else -> HomeContent(profile, ui.tasks, actions, onOpenFilter, onOpenInsights, onOpenTimer, onAddTask)
        }
    }
    if (showQuick) QuickAddDialog(onDismiss = { showQuick = false }, onAdd = { t, p -> onQuickAdd(t, p); showQuick = false })
}

@Composable
private fun HomeContent(
    profile: UserProfile?, tasks: List<Task>, actions: TaskActions,
    onOpenFilter: (String) -> Unit, onOpenInsights: () -> Unit, onOpenTimer: () -> Unit, onAddTask: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val today = LocalDate.now(zone)
    val stats = TaskLogic.stats(tasks, now, zone)
    val hour = LocalTime.now(zone).hour
    val first = profile?.firstName.orEmpty()
    val greeting = TaskLogic.greeting(hour) + if (first.isNotBlank()) ", $first" else ""
    val focus = TaskLogic.todaysFocus(tasks, now, zone)
    val upcoming = TaskLogic.upcoming(tasks, now, 4, zone)
    val reduce = LocalReduceMotion.current
    val appear = remember { MutableTransitionState(reduce).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = appear,
        enter = if (reduce) fadeIn(tween(0)) else fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.96f)
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Sun.Golden),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(greeting, style = MaterialTheme.typography.headlineSmall, color = Sun.Navy)
                            Text(DateFmt.day(today), style = MaterialTheme.typography.bodyMedium, color = Sun.Navy)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${stats.active} active task${if (stats.active == 1) "" else "s"}, ${stats.dueToday} due today",
                                style = MaterialTheme.typography.bodyMedium, color = Sun.Navy
                            )
                        }
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Sun.Navy, modifier = Modifier.size(48.dp))
                    }
                }
            }
            if (tasks.isEmpty()) {
                item {
                    EmptyState(Icons.Default.WbSunny, "No tasks yet", "Add a task to see your day, deadlines and progress here.") {
                        Button(onClick = onAddTask) { Text("Create Your First Task") }
                    }
                }
                return@LazyColumn
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Active tasks", stats.active, Icons.Default.Checklist, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { onOpenFilter("ACTIVE") }
                        StatCard("Completed", stats.completed, Icons.Default.CheckCircle, successColor(), Modifier.weight(1f)) { onOpenFilter("COMPLETED") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("High priority", stats.highActive, Icons.Default.Flag, priorityColor(Priority.HIGH), Modifier.weight(1f)) { onOpenFilter("HIGH") }
                        StatCard("Due today", stats.dueToday, Icons.Default.Schedule, priorityColor(Priority.MEDIUM), Modifier.weight(1f)) { onOpenFilter("TODAY") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Overdue", stats.overdue, Icons.Default.Warning, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { onOpenFilter("OVERDUE") }
                        CompletionCard(stats.completionPct, stats.completed, stats.total, Modifier.weight(1f), onOpenInsights)
                    }
                }
            }
            if (stats.active == 0) {
                item { CelebrationCard() }
            }
            item {
                Button(onClick = onOpenTimer, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.Timer, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Start Focus Timer")
                }
            }
            item { SectionHeader("Today's focus", focus.size) }
            if (focus.isEmpty()) {
                item { Text("Nothing urgent right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(focus.take(5), key = { "f" + it.id }) { TaskCard(it, actions) }
            if (focus.size > 5) {
                item { TextButton(onClick = { onOpenFilter("ACTIVE") }) { Text("See all ${focus.size}") } }
            }
            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming deadlines", upcoming.size) }
                items(upcoming, key = { "u" + it.id }) { TaskCard(it, actions) }
            }
            item { SectionHeader("Quick actions") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onAddTask, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add Task")
                        }
                        FilledTonalButton(onClick = { onOpenFilter("ALL") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.List, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("View All")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onOpenTimer, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Timer, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Focus Timer")
                        }
                        FilledTonalButton(onClick = onOpenInsights, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Insights, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Insights")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(8.dp))
            Text("$value", style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompletionCard(pct: Int, done: Int, total: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(pct / 100f, 64.dp, 8.dp) { Text("$pct%", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Completion", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$done of $total", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** Small celebration when every task is complete. The sun only turns when animations are enabled. */
@Composable
private fun CelebrationCard() {
    val reduce = LocalReduceMotion.current
    val angle by rememberInfiniteTransition(label = "celebrate").animateFloat(
        initialValue = 0f, targetValue = if (reduce) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Restart), label = "sunAngle"
    )
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Sun.Amber, modifier = Modifier.size(40.dp).rotate(angle))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("All tasks complete!", style = MaterialTheme.typography.titleMedium)
                Text("Nice work. Add something new or enjoy the sunshine.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (String, Priority) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onAdd(title, priority) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
