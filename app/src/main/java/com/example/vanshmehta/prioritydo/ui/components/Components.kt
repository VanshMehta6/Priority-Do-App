@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vanshmehta.prioritydo.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.ui.theme.Sun
import com.example.vanshmehta.prioritydo.util.DateFmt
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.example.vanshmehta.prioritydo.util.combineDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

val LocalReduceMotion = compositionLocalOf { false }

@Composable
private fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun successColor(): Color = if (isDark()) Sun.DarkSuccess else Sun.Success

/** Text-safe priority colours (labels are always shown next to the colour, never colour alone). */
@Composable
fun priorityColor(p: Priority): Color = if (isDark()) when (p) {
    Priority.HIGH -> Color(0xFFFF8A84)
    Priority.MEDIUM -> Color(0xFFF4B942)
    Priority.LOW -> Color(0xFF58C896)
} else when (p) {
    Priority.HIGH -> Color(0xFFC9302C)
    Priority.MEDIUM -> Color(0xFF946000)
    Priority.LOW -> Color(0xFF237A52)
}

data class TaskActions(
    val onToggle: (Task) -> Unit,
    val onDelete: (Task) -> Unit,
    val onOpen: (Task) -> Unit,
    val onEdit: (Task) -> Unit
)

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val c by animateColorAsState(priorityColor(priority), label = "priorityColor")
    Surface(color = c.copy(alpha = 0.14f), shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = c, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(priority.label, color = c, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AnimatedCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (checked) 1f else 0f,
        if (reduce) snap() else spring(Spring.DampingRatioMediumBouncy), label = "checkScale"
    )
    val ok = successColor()
    val fill by animateColorAsState(if (checked) ok else Color.Transparent, label = "checkFill")
    val border by animateColorAsState(if (checked) ok else MaterialTheme.colorScheme.outline, label = "checkBorder")
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = if (checked) "Completed. Tap to reopen" else "Not completed. Tap to complete" }
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(26.dp).border(2.dp, border, CircleShape).background(fill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp).scale(scale))
        }
    }
}

@Composable
fun TaskCard(task: Task, actions: TaskActions, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(task.id) { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val overdue = TaskLogic.isOverdue(task, now)
    val alpha by animateFloatAsState(if (task.isCompleted) 0.62f else 1f, label = "cardAlpha")
    val reduce = LocalReduceMotion.current
    Card(
        onClick = { actions.onOpen(task) },
        modifier = modifier.fillMaxWidth().then(if (reduce) Modifier else Modifier.animateContentSize()),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).alpha(alpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedCheck(task.isCompleted, { actions.onToggle(task) })
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(task.category.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val schedule = DateFmt.schedule(task)
                    if (overdue || schedule != null) {
                        Text(
                            (if (overdue) "Overdue - " else "") + (schedule ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PriorityBadge(task.priority)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse details" else "Expand details"
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = if (reduce) fadeIn(snap()) else expandVertically() + fadeIn(),
                exit = if (reduce) fadeOut(snap()) else shrinkVertically() + fadeOut()
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)) {
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text("Importance: ${task.importance.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { actions.onEdit(task) }) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Edit")
                        }
                        TextButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete task?", "\"${task.title}\" will be removed. You can undo right after.", "Delete",
            onConfirm = { confirmDelete = false; actions.onDelete(task) }, onDismiss = { confirmDelete = false }
        )
    }
}

/** Swipe right = complete/reopen, swipe left = delete (the ViewModel offers Undo in a snackbar). */
@Composable
fun SwipeableTaskCard(task: Task, actions: TaskActions, modifier: Modifier = Modifier) {
    val currentTask by rememberUpdatedState(task)
    val currentActions by rememberUpdatedState(actions)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { currentActions.onDelete(currentTask); true }
                SwipeToDismissBoxValue.StartToEnd -> { currentActions.onToggle(currentTask); false }
                else -> false
            }
        }
    )
    val ok = successColor()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val color = when (dir) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.StartToEnd -> ok
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        if (dir == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Check,
                        contentDescription = if (dir == SwipeToDismissBoxValue.EndToStart) "Delete task" else "Toggle complete",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        TaskCard(task, actions)
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (count != null) "$title ($count)" else title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val reduce = LocalReduceMotion.current
    val pulse by rememberInfiniteTransition(label = "emptyPulse").animateFloat(
        initialValue = 1f, targetValue = if (reduce) 1f else 1.1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "emptyScale"
    )
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).scale(pulse).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    EmptyState(Icons.Default.ErrorOutline, "Something went wrong", message, modifier)
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Skeleton-style placeholder cards shown while tasks load. */
@Composable
fun SkeletonList(count: Int = 4, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val a by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f, targetValue = if (reduce) 0.35f else 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "skeletonAlpha"
    )
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Box(
                Modifier.fillMaxWidth().height(72.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = a))
            )
        }
    }
}

@Composable
fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Offline - showing saved data. Changes sync when you reconnect.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Circular progress ring drawn on a Canvas, with centred content. */
@Composable
fun ProgressRing(
    fraction: Float, size: Dp, strokeWidth: Dp, modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    track: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit = {}
) {
    val reduce = LocalReduceMotion.current
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), if (reduce) snap() else tween(600), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            if (f > 0f) drawArc(color, -90f, 360f * f, false, Offset(inset, inset), arcSize, style = stroke)
        }
        content()
    }
}

/** Date, then (optionally) time. Returns epoch millis in the device time zone. Only opened on user tap. */
@Composable
fun DateTimePickerDialog(
    initial: Long?, title: String,
    onDismiss: () -> Unit, onConfirm: (Long) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val init = Instant.ofEpochMilli(initial ?: System.currentTimeMillis()).atZone(zone)
    var step by remember { mutableIntStateOf(0) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = init.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val timeState = rememberTimePickerState(
        initialHour = init.hour, initialMinute = init.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { if (dateState.selectedDateMillis != null) step = 1 }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) { DatePicker(state = dateState, title = { Text(title, Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { d -> onConfirm(combineDateTime(d, timeState.hour, timeState.minute)) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Back") } }
        )
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
