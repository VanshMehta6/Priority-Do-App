package com.example.vanshmehta.prioritydo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.data.repository.TaskRepository
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.google.firebase.database.DatabaseException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.google.android.gms.tasks.Task as GmsTask

data class TaskUiState(
    val loading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val online: Boolean = true
)

data class UiEvent(val text: String, val undoable: Boolean = false)

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository()

    private val _ui = MutableStateFlow(TaskUiState())
    val ui: StateFlow<TaskUiState> = _ui.asStateFlow()

    private val _saving = MutableStateFlow(false)
    /** True while a form save is in flight; the form disables its button to block duplicates. */
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var uid: String? = null
    private var jobs: List<Job> = emptyList()
    private var lastDeleted: Task? = null

    /** Called whenever the authenticated user changes. Cancels old listeners (no duplicates). */
    fun setUser(newUid: String?) {
        if (newUid == uid) return
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        uid = newUid
        lastDeleted = null
        _saving.value = false
        _ui.value = TaskUiState(loading = newUid != null)
        if (newUid == null) return
        jobs = listOf(
            viewModelScope.launch {
                repo.observeTasks(newUid).collect { r ->
                    r.onSuccess { list -> _ui.update { it.copy(loading = false, tasks = list, error = null) } }
                        .onFailure { e -> _ui.update { it.copy(loading = false, error = friendly(e)) } }
                }
            },
            viewModelScope.launch { repo.observeConnection().collect { c -> _ui.update { it.copy(online = c) } } },
            viewModelScope.launch {
                // Offline with an empty cache never delivers data: stop the spinner eventually.
                delay(8000)
                _ui.update { if (it.loading) it.copy(loading = false) else it }
            }
        )
    }

    private fun emit(text: String, undoable: Boolean = false) {
        _events.tryEmit(UiEvent(text, undoable))
    }

    private fun friendly(e: Throwable): String =
        if (e.message?.contains("permission", ignoreCase = true) == true) "Permission denied. Check your database rules and sign-in."
        else if (e is DatabaseException) "Database error. Please try again."
        else "Something went wrong. Please try again."

    /** Local writes are queued by Firebase; report a later rejection (e.g. rules) when it arrives. */
    private fun fire(task: GmsTask<Void>) {
        task.addOnFailureListener { emit("Sync problem: ${friendly(it)}") }
    }

    /**
     * Saves a new or edited task. Awaits the server for a few seconds; offline it reports that the
     * change was stored locally. Ignored while a save is already running (no duplicate submits).
     */
    fun saveTask(task: Task, onSuccess: () -> Unit = {}) {
        val u = uid ?: return
        if (_saving.value) return
        _saving.value = true
        val now = System.currentTimeMillis()
        val isNew = task.id.isBlank()
        val id = if (isNew) repo.newId(u) else task.id
        val toSave = task.copy(
            id = id, ownerUid = u, title = task.title.trim(),
            createdAt = if (isNew) now else task.createdAt, updatedAt = now
        )
        viewModelScope.launch {
            try {
                val write = if (isNew) repo.create(u, toSave) else repo.update(u, id, toSave.editableMap())
                val confirmed = withTimeoutOrNull(WRITE_TIMEOUT_MS) { write.await(); true } ?: false
                if (confirmed) {
                    emit(if (isNew) "Task added" else "Task updated")
                } else {
                    fire(write)
                    emit("Saved on this device. It will sync when you are back online.")
                }
                onSuccess()
            } catch (e: Exception) {
                emit("Could not save the task: ${friendly(e)}")
            } finally {
                _saving.value = false
            }
        }
    }

    fun quickAdd(title: String, priority: Priority) {
        if (title.isBlank()) return emit("Enter a title")
        saveTask(Task(title = title, priority = priority))
    }

    /** Complete or reopen. completedAt is set on completion and removed on reopen. */
    fun toggleComplete(t: Task) {
        val u = uid ?: return
        val now = System.currentTimeMillis()
        val done = !t.isCompleted
        val fields = mapOf<String, Any?>(
            "isCompleted" to done,
            "completedAt" to (if (done) now else null),
            "updatedAt" to now
        )
        if (done && t.recurrence != com.example.vanshmehta.prioritydo.data.model.Recurrence.NONE && !t.recurrenceSpawned) {
            val next = TaskLogic.nextOccurrence(t)
            if (next != null) {
                val nt = next.copy(id = repo.newId(u), ownerUid = u, createdAt = now, updatedAt = now)
                fire(repo.completeWithNext(u, t.id, fields, nt))
                emit("Completed. Next occurrence created.")
                return
            }
        }
        fire(repo.update(u, t.id, fields))
        emit(if (done) "Task completed" else "Task reopened")
    }

    fun deleteTask(t: Task) {
        val u = uid ?: return
        lastDeleted = t
        fire(repo.delete(u, t.id))
        emit("Task deleted", undoable = true)
    }

    /** Restores the last deleted task from the in-memory copy (this session only). */
    fun undoDelete() {
        val u = uid ?: return
        val t = lastDeleted ?: return
        lastDeleted = null
        fire(repo.create(u, t.copy(ownerUid = u)))
        emit("Task restored")
    }

    private companion object {
        const val WRITE_TIMEOUT_MS = 5000L
    }
}
