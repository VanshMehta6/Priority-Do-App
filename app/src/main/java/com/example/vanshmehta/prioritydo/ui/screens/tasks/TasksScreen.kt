@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.vanshmehta.prioritydo.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.data.model.Category
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.ui.components.EmptyState
import com.example.vanshmehta.prioritydo.ui.components.ErrorState
import com.example.vanshmehta.prioritydo.ui.components.OfflineBanner
import com.example.vanshmehta.prioritydo.ui.components.SectionHeader
import com.example.vanshmehta.prioritydo.ui.components.SkeletonList
import com.example.vanshmehta.prioritydo.ui.components.SwipeableTaskCard
import com.example.vanshmehta.prioritydo.ui.components.TaskActions
import com.example.vanshmehta.prioritydo.util.SortMode
import com.example.vanshmehta.prioritydo.util.StatusFilter
import com.example.vanshmehta.prioritydo.util.TaskFilter
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.example.vanshmehta.prioritydo.viewmodel.TaskUiState

@Composable
fun TasksScreen(
    ui: TaskUiState,
    initialFilter: String,
    actions: TaskActions
) {
    var filter by rememberSaveable(initialFilter) { mutableStateOf(TaskLogic.presetFilter(initialFilter)) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    var categoryMenu by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Tasks", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { searching = !searching; if (!searching) filter = filter.copy(query = "") }) {
                    Icon(if (searching) Icons.Default.Close else Icons.Default.Search, contentDescription = if (searching) "Close search" else "Search tasks")
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "Sort tasks") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.values().forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.label + if (filter.sort == m) "  (selected)" else "") },
                                onClick = { filter = filter.copy(sort = m); sortMenu = false }
                            )
                        }
                    }
                }
            }
        )
        if (!ui.online) OfflineBanner()
        if (searching) {
            OutlinedTextField(
                value = filter.query, onValueChange = { filter = filter.copy(query = it) },
                label = { Text("Search title or description") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StatusFilter.values().toList()) { s ->
                FilterChip(selected = filter.status == s, onClick = { filter = filter.copy(status = s) }, label = { Text(s.label) })
            }
            items(Priority.values().toList()) { p ->
                FilterChip(
                    selected = filter.priority == p,
                    onClick = { filter = filter.copy(priority = if (filter.priority == p) null else p) },
                    label = { Text(p.label) }
                )
            }
            item {
                FilterChip(selected = filter.overdueOnly, onClick = { filter = filter.copy(overdueOnly = !filter.overdueOnly) }, label = { Text("Overdue") })
            }
            item {
                FilterChip(selected = filter.dueTodayOnly, onClick = { filter = filter.copy(dueTodayOnly = !filter.dueTodayOnly) }, label = { Text("Due today") })
            }
            item {
                Box {
                    FilterChip(
                        selected = filter.category != null,
                        onClick = { categoryMenu = true },
                        label = { Text(filter.category?.label ?: "Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose category") }
                    )
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(text = { Text("Any category") }, onClick = { filter = filter.copy(category = null); categoryMenu = false })
                        Category.values().forEach { c ->
                            DropdownMenuItem(text = { Text(c.label) }, onClick = { filter = filter.copy(category = c); categoryMenu = false })
                        }
                    }
                }
            }
        }
        val now = System.currentTimeMillis()
        when {
            ui.loading -> SkeletonList()
            ui.error != null -> ErrorState(ui.error)
            else -> {
                val sorted = TaskLogic.applyFilter(ui.tasks, filter, now)
                val (active, completed) = TaskLogic.sections(sorted)
                if (sorted.isEmpty()) {
                    if (ui.tasks.isEmpty()) {
                        EmptyState(Icons.Default.Checklist, "No tasks yet", "Tap + to add your first task.")
                    } else {
                        EmptyState(Icons.Default.SearchOff, "No matching tasks", "Try changing the filters or the search.") {
                            TextButton(onClick = { filter = TaskFilter(); searching = false }) { Text("Clear filters") }
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (active.isNotEmpty()) {
                            item(key = "hdr_active") { SectionHeader("Active", active.size) }
                            items(active, key = { it.id }) { t -> SwipeableTaskCard(t, actions, Modifier.animateItem()) }
                        }
                        if (completed.isNotEmpty()) {
                            item(key = "hdr_completed") { SectionHeader("Completed", completed.size) }
                            items(completed, key = { it.id }) { t -> SwipeableTaskCard(t, actions, Modifier.animateItem()) }
                        }
                    }
                }
            }
        }
    }
}
