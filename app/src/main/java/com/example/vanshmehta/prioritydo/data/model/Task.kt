package com.example.vanshmehta.prioritydo.data.model

import com.google.firebase.database.DataSnapshot

enum class Priority(val label: String, val rank: Int) {
    HIGH("High", 0), MEDIUM("Medium", 1), LOW("Low", 2)
}

enum class Importance(val label: String) {
    CRITICAL("Critical"), IMPORTANT("Important"), NORMAL("Normal")
}

enum class Category(val label: String) {
    ACADEMIC("Academic"),
    WORK_CAREER("Work and Career"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    FINANCE("Finance"),
    HOUSEHOLD("Household"),
    OTHER("Other")
}

/**
 * Recurrence model: when a recurring task is completed, the next occurrence is created once
 * (see TaskRepository.completeWithNext). [Task.recurrenceSpawned] marks the completed copy so
 * reopening and completing it again can never create a second follow-up.
 */
enum class Recurrence(val label: String) {
    NONE("Does not repeat"), DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly")
}

/**
 * All timestamps are Unix epoch milliseconds (UTC). Null = not set.
 * Enum values are stored in Firebase by their [Enum.name].
 */
data class Task(
    val id: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val importance: Importance = Importance.NORMAL,
    val category: Category = Category.OTHER,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val startAt: Long? = null,
    val dueAt: Long? = null,
    val durationMinutes: Int? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceSpawned: Boolean = false,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /** Fields the user can change in the form. Null values delete the child on updateChildren. */
    fun editableMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "priority" to priority.name,
        "importance" to importance.name,
        "category" to category.name,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt,
        "startAt" to startAt,
        "dueAt" to dueAt,
        "durationMinutes" to durationMinutes,
        "recurrence" to recurrence.name,
        "notes" to notes,
        "updatedAt" to updatedAt
    )

    fun toMap(): Map<String, Any?> =
        editableMap() + mapOf(
            "ownerUid" to ownerUid,
            "createdAt" to createdAt,
            "recurrenceSpawned" to recurrenceSpawned
        )

    companion object {
        private fun <T> safe(block: () -> T?): T? = try { block() } catch (e: Exception) { null }

        private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == value } ?: default

        /** Returns null for records too malformed to show (no key or no title). */
        fun fromSnapshot(s: DataSnapshot): Task? {
            val key = s.key ?: return null
            fun str(k: String) = safe { s.child(k).getValue(String::class.java) }
            fun lng(k: String) = safe { s.child(k).getValue(Long::class.java) }
            fun bool(k: String) = safe { s.child(k).getValue(Boolean::class.java) }
            val title = str("title")?.takeIf { it.isNotBlank() } ?: return null
            return Task(
                id = key,
                ownerUid = str("ownerUid").orEmpty(),
                title = title,
                description = str("description").orEmpty(),
                priority = enumOr(str("priority"), Priority.MEDIUM),
                importance = enumOr(str("importance"), Importance.NORMAL),
                category = enumOr(str("category"), Category.OTHER),
                isCompleted = bool("isCompleted") ?: false,
                completedAt = lng("completedAt"),
                startAt = lng("startAt"),
                dueAt = lng("dueAt"),
                durationMinutes = lng("durationMinutes")?.toInt(),
                recurrence = enumOr(str("recurrence"), Recurrence.NONE),
                recurrenceSpawned = bool("recurrenceSpawned") ?: false,
                notes = str("notes").orEmpty(),
                createdAt = lng("createdAt") ?: 0L,
                updatedAt = lng("updatedAt") ?: 0L
            )
        }
    }
}
