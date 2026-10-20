package com.example.vanshmehta.prioritydo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vanshmehta.prioritydo.data.model.UserProfile
import com.example.vanshmehta.prioritydo.data.repository.AuthRepository
import com.example.vanshmehta.prioritydo.util.Validators
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val checking: Boolean = true,
    val user: FirebaseUser? = null,
    val profile: UserProfile? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.authState()
                .flatMapLatest { user ->
                    if (user == null) flowOf<Pair<FirebaseUser?, UserProfile?>>(null to null)
                    else repo.observeProfile(user)
                        .onStart { emit(AuthRepository.fallbackProfile(user)) }
                        .map { p -> user to p }
                }
                .collect { (u, p) -> _state.update { it.copy(checking = false, user = u, profile = p) } }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    fun login(email: String, password: String) {
        if (!Validators.isEmail(email)) return fail("Enter a valid email address")
        if (password.isEmpty()) return fail("Enter your password")
        run { repo.login(email.trim(), password) }
    }

    fun register(name: String, email: String, password: String, confirm: String) {
        if (name.isBlank()) return fail("Name is required")
        if (name.trim().length > 100) return fail("Name must be 100 characters or fewer")
        if (!Validators.isEmail(email)) return fail("Enter a valid email address")
        Validators.passwordError(password)?.let { return fail(it) }
        if (password != confirm) return fail("Passwords do not match")
        run { repo.register(name.trim(), email.trim(), password) }
    }

    fun resetPassword(email: String) {
        if (!Validators.isEmail(email)) return fail("Enter a valid email address to reset your password")
        run(successMessage = "Password reset email sent. Check your inbox.") { repo.sendReset(email.trim()) }
    }

    fun logout() {
        repo.logout()
        _state.update { AuthUiState(checking = false) }
    }

    fun updateDisplayName(name: String) {
        val user = _state.value.user ?: return
        if (name.isBlank()) return fail("Name cannot be empty")
        repo.updateDisplayName(user, name.trim().take(100))
        _state.update { it.copy(info = "Profile updated", error = null) }
    }

    private fun fail(msg: String) = _state.update { it.copy(error = msg, info = null) }

    private fun run(successMessage: String? = null, block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(loading = false, info = successMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = AuthRepository.friendlyError(e)) }
            }
        }
    }
}
