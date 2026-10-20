@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vanshmehta.prioritydo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vanshmehta.prioritydo.ui.theme.Sun
import com.example.vanshmehta.prioritydo.viewmodel.AuthUiState

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Sun.Golden), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Sun.Navy, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text("PriorityDo", color = Sun.Navy, style = MaterialTheme.typography.headlineMedium)
            Text("Sun Edition", color = Sun.Navy, style = MaterialTheme.typography.bodyMedium)
            Text("Vansh Mehta | C037", color = Sun.Navy, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Sun.Navy)
        }
    }
}

@Composable
private fun Brand(subtitle: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(Sun.Golden, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Sun.Navy, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("PriorityDo", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("Sun Edition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Vansh Mehta | C037", color = Sun.Navy, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text(subtitle, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun Messages(state: AuthUiState) {
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp))
    }
    state.info?.let {
        Text(it, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onReset: (String) -> Unit,
    onGoRegister: () -> Unit,
    onClearMessages: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showReset by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Brand("Sign in to continue")
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        PasswordField(password, { password = it }, "Password")
        Messages(state)
        Button(onClick = { onLogin(email, password) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Sign in")
        }
        TextButton(onClick = { onClearMessages(); showReset = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Forgot password?") }
        TextButton(onClick = { onClearMessages(); onGoRegister() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("New here? Create an account") }
    }
    if (showReset) {
        var resetEmail by rememberSaveable { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset password") },
            text = {
                Column {
                    Text("We will email you a link to reset your password.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail, onValueChange = { resetEmail = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onReset(resetEmail); showReset = false }) { Text("Send link") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (name: String, email: String, password: String, confirm: String) -> Unit,
    onGoLogin: () -> Unit,
    onClearMessages: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Brand("Create your account")
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(100) }, label = { Text("Name") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        PasswordField(password, { password = it }, "Password (min 6 characters)")
        PasswordField(confirm, { confirm = it }, "Confirm password")
        Messages(state)
        Button(
            onClick = { onRegister(name, email, password, confirm) },
            enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Create account")
        }
        TextButton(onClick = { onClearMessages(); onGoLogin() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Already registered? Sign in") }
    }
}
