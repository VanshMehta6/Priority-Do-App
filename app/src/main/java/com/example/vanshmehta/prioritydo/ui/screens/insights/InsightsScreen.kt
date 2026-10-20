@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vanshmehta.prioritydo.ui.screens.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.ui.components.EmptyState
import com.example.vanshmehta.prioritydo.ui.components.ProgressRing
import com.example.vanshmehta.prioritydo.ui.components.SkeletonList
import com.example.vanshmehta.prioritydo.ui.components.priorityColor
import com.example.vanshmehta.prioritydo.ui.components.successColor
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.example.vanshmehta.prioritydo.viewmodel.TaskUiState
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(ui: TaskUiState) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Insights", fontWeight = FontWeight.Bold) })
        if (ui.loading) { SkeletonList(); return@Column }
        if (ui.tasks.isEmpty()) {
            EmptyState(Icons.Default.Insights, "No data yet", "Add and complete some tasks to see your insights.")
            return@Column
        }
        val now = System.currentTimeMillis()
        val s = TaskLogic.stats(ui.tasks, now)
        val weekly = TaskLogic.weeklyCompletion(ui.tasks, now)
        val maxWeek = (weekly.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val byPriority = TaskLogic.byPriority(ui.tasks)
        val byCategory = TaskLogic.byCategory(ui.tasks)
        val ok = successColor()

        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(s.completionPct / 100f, 96.dp, 12.dp) {
                        Text("${s.completionPct}%", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Completion", style = MaterialTheme.typography.titleMedium)
                        Text("${s.completed} of ${s.total} tasks completed", style = MaterialTheme.typography.bodyMedium)
                        Text("Completed today: ${s.completedToday}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            InsightCard {
                Text("Summary", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth()) {
                    Metric("Total", s.total.toString(), Modifier.weight(1f))
                    Metric("Completed", s.completed.toString(), Modifier.weight(1f))
                    Metric("Active", s.active.toString(), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    Metric("Overdue", s.overdue.toString(), Modifier.weight(1f))
                    Metric("Done today", s.completedToday.toString(), Modifier.weight(1f))
                    Metric("High-priority done", "${s.highCompletionPct}%", Modifier.weight(1f))
                }
                if (s.highTotal == 0) {
                    Text("No high-priority tasks yet, so the rate shows 0%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            InsightCard {
                Text("Completed vs active", style = MaterialTheme.typography.titleMedium)
                BarRow("Completed", s.completed, s.total, ok)
                BarRow("Active", s.active, s.total, MaterialTheme.colorScheme.secondary)
            }
            InsightCard {
                Text("Tasks by priority", style = MaterialTheme.typography.titleMedium)
                byPriority.forEach { (p, n) -> BarRow(p.label, n, ui.tasks.size, priorityColor(p)) }
            }
            InsightCard {
                Text("Tasks by category", style = MaterialTheme.typography.titleMedium)
                byCategory.forEach { (c, n) -> BarRow(c.label, n, ui.tasks.size, MaterialTheme.colorScheme.tertiary) }
            }
            InsightCard {
                Text("Completed in the last 7 days", style = MaterialTheme.typography.titleMedium)
                if (weekly.all { it.second == 0 }) {
                    Text("No completions recorded this week yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom
                ) {
                    weekly.forEach { (d, n) ->
                        val frac by animateFloatAsState(n.toFloat() / maxWeek, label = "bar")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                            Text("$n", style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.width(24.dp).height((84 * frac + 2).dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondary))
                            Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Based on the time each task was marked complete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsightCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Labelled horizontal bar with a colour swatch legend and exact count. */
@Composable
private fun BarRow(label: String, count: Int, total: Int, color: Color) {
    val frac by animateFloatAsState(if (total == 0) 0f else count.toFloat() / total, label = "barRow")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(color))
        }
    }
}
