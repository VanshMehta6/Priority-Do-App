@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.vanshmehta.prioritydo.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.BuildConfig
import com.example.vanshmehta.prioritydo.data.model.UserProfile
import com.example.vanshmehta.prioritydo.data.prefs.ThemeMode
import com.example.vanshmehta.prioritydo.ui.components.ConfirmDialog
import com.example.vanshmehta.prioritydo.ui.theme.Sun
import com.example.vanshmehta.prioritydo.util.DateFmt

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onUpdateName: (String) -> Unit,
    onLogout: () -> Unit
) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Profile", fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit display name") } }
        )
        if (profile == null) return@Column
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(Sun.Golden, CircleShape), contentAlignment = Alignment.Center) {
                    Text(profile.displayName.take(1).uppercase(), color = Sun.Navy, style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Info("Display name", profile.displayName)
                    Info("Email", profile.email)
                    Info("Account created", if (profile.createdAt > 0) DateFmt.date(profile.createdAt) else "Not available")
                }
            }
            HorizontalDivider()
            Text("Theme", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { m -> FilterChip(themeMode == m, { onThemeMode(m) }, { Text(m.label) }) }
            }
            HorizontalDivider()
            Info("App version", BuildConfig.VERSION_NAME)
            TextButton(onClick = { about = true }) { Text("About PriorityDo") }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            ) { Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Sign out") }
        }
    }
    if (confirmLogout) ConfirmDialog("Sign out?", "You will need to sign in again to see your tasks.", "Sign out",
        onConfirm = { confirmLogout = false; onLogout() }, onDismiss = { confirmLogout = false })
    if (about) AlertDialog(
        onDismissRequest = { about = false },
        title = { Text("PriorityDo - Sun Edition") },
        text = {
            Text("A task manager that helps you prioritise, schedule and finish work, with a focus timer and productivity insights.\n\nDeveloped by Vansh Mehta.\nVersion ${BuildConfig.VERSION_NAME}")
        },
        confirmButton = { TextButton(onClick = { about = false }) { Text("Close") } }
    )
    if (editing) {
        var name by rememberSaveable { mutableStateOf(profile?.displayName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit display name") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text("Name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onUpdateName(name); editing = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
