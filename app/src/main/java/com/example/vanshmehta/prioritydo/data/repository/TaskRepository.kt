package com.example.vanshmehta.prioritydo.data.repository

import com.example.vanshmehta.prioritydo.data.model.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.android.gms.tasks.Task as GmsTask

/**
 * Realtime Database access, always scoped to /tasks/{uid}. Writes return the Firebase task so
 * callers decide whether to await it (an await never completes while offline).
 */
class TaskRepository(private val db: FirebaseDatabase = FirebaseDatabase.getInstance()) {

    private fun tasksRef(uid: String): DatabaseReference = db.getReference("tasks").child(uid)

    fun newId(uid: String): String = tasksRef(uid).push().key ?: error("Could not generate task id")

    /** Live task list. The listener is removed when the collector is cancelled. */
    fun observeTasks(uid: String): Flow<Result<List<Task>>> = callbackFlow {
        val ref = tasksRef(uid)
        ref.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(Result.success(snapshot.children.mapNotNull { Task.fromSnapshot(it) }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeConnection(): Flow<Boolean> = callbackFlow {
        val ref = db.getReference(".info/connected")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(s.getValue(Boolean::class.java) == true) }
            override fun onCancelled(e: DatabaseError) { /* ignore */ }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    fun create(uid: String, task: Task): GmsTask<Void> =
        tasksRef(uid).child(task.id).setValue(task.toMap().filterValues { it != null })

    /** Partial update via updateChildren. A null value removes that field. */
    fun update(uid: String, id: String, fields: Map<String, Any?>): GmsTask<Void> {
        @Suppress("UNCHECKED_CAST")
        return tasksRef(uid).child(id).updateChildren(fields as Map<String, Any>)
    }

    fun delete(uid: String, id: String): GmsTask<Void> = tasksRef(uid).child(id).removeValue()

    /**
     * Atomic multi-path update: completes [completed] and creates [next] in one write, so a
     * recurring task can never spawn a duplicate (the completed copy is flagged recurrenceSpawned).
     */
    fun completeWithNext(uid: String, completedId: String, completedFields: Map<String, Any?>, next: Task): GmsTask<Void> {
        val updates = HashMap<String, Any?>()
        completedFields.forEach { (k, v) -> updates["$completedId/$k"] = v }
        updates["$completedId/recurrenceSpawned"] = true
        next.toMap().filterValues { it != null }.forEach { (k, v) -> updates["${next.id}/$k"] = v }
        @Suppress("UNCHECKED_CAST")
        return tasksRef(uid).updateChildren(updates as Map<String, Any>)
    }
}
