package com.example.vanshmehta.prioritydo.util

import com.example.vanshmehta.prioritydo.data.model.Category
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Recurrence
import com.example.vanshmehta.prioritydo.data.model.Task
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SortMode(val label: String) {
    PRIORITY("Priority"),
    DEADLINE("Deadline"),
    NEWEST("Recently created"),
    OLDEST("Oldest first"),
    ALPHA("Alphabetical")
}

enum class StatusFilter(val label: String) { ALL("All"), ACTIVE("Active"), COMPLETED("Completed") }

data class TaskFilter(
    val status: StatusFilter = StatusFilter.ALL,
    val priority: Priority? = null,
    val category: Category? = null,
    val overdueOnly: Boolean = false,
    val dueTodayOnly: Boolean = false,
    val query: String = "",
    val sort: SortMode = SortMode.PRIORITY
) : Serializable {
    /** Number of advanced filters in use (badge on the filter button). */
    val advancedCount: Int
        get() = listOf(priority != null, category != null, overdueOnly, dueTodayOnly).count { it }
}

data class Stats(
    val total: Int,
    val active: Int,
    val completed: Int,
    val highActive: Int,
    val dueToday: Int,
    val overdue: Int,
    val completedToday: Int,
    val completionPct: Int,
    val highTotal: Int,
    val highCompleted: Int,
    val highCompletionPct: Int
)

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isEmail(e: String) = EMAIL.matches(e.trim())
    fun passwordError(p: String): String? = if (p.length < 6) "Password must be at least 6 characters" else null
}

object TaskLogic {
    const val MAX_DURATION_MIN = 1440

    fun isOverdue(t: Task, now: Long): Boolean = !t.isCompleted && t.dueAt != null && t.dueAt < now

    fun isDueToday(t: Task, today: LocalDate, zone: ZoneId): Boolean =
        !t.isCompleted && t.dueAt?.toLocalDate(zone) == today

    /**
     * Default order: High > Medium > Low. Inside a priority: overdue active tasks, then earliest
     * deadline, then earliest start time, then tasks with no dates, finally creation time.
     */
    fun defaultComparator(now: Long): Comparator<Task> = compareBy<Task>(
        { it.priority.rank },
        { if (isOverdue(it, now)) 0 else 1 },
        { it.dueAt == null },
        { it.dueAt },
        { it.startAt == null },
        { it.startAt },
        { it.createdAt }
    )

    /** Returns a new list. The tasks themselves (priority, deadline, importance) are untouched. */
    fun sort(tasks: List<Task>, mode: SortMode, now: Long): List<Task> = when (mode) {
        SortMode.PRIORITY -> tasks.sortedWith(defaultComparator(now))
        SortMode.DEADLINE -> tasks.sortedWith(compareBy<Task>({ it.dueAt == null }, { it.dueAt }, { it.priority.rank }, { it.createdAt }))
        SortMode.NEWEST -> tasks.sortedByDescending { it.createdAt }
        SortMode.OLDEST -> tasks.sortedBy { it.createdAt }
        SortMode.ALPHA -> tasks.sortedBy { it.title.lowercase() }
    }

    /** Filters compose: every active filter must match. Search covers title and description. */
    fun applyFilter(tasks: List<Task>, f: TaskFilter, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        val today = now.toLocalDate(zone)
        val q = f.query.trim().lowercase()
        val list = tasks.filter { t ->
            val statusOk = when (f.status) {
                StatusFilter.ACTIVE -> !t.isCompleted
                StatusFilter.COMPLETED -> t.isCompleted
                StatusFilter.ALL -> true
            }
            statusOk &&
                (f.priority == null || t.priority == f.priority) &&
                (f.category == null || t.category == f.category) &&
                (!f.overdueOnly || isOverdue(t, now)) &&
                (!f.dueTodayOnly || isDueToday(t, today, zone)) &&
                (q.isEmpty() || t.title.lowercase().contains(q) || t.description.lowercase().contains(q))
        }
        return sort(list, f.sort, now)
    }

    /** Splits an already-sorted list into (active, completed) sections. */
    fun sections(sorted: List<Task>): Pair<List<Task>, List<Task>> = sorted.partition { !it.isCompleted }

    /** Preset names used by dashboard cards and quick links. */
    fun presetFilter(name: String): TaskFilter = when (name) {
        "ACTIVE" -> TaskFilter(status = StatusFilter.ACTIVE)
        "HIGH" -> TaskFilter(status = StatusFilter.ACTIVE, priority = Priority.HIGH)
        "TODAY" -> TaskFilter(status = StatusFilter.ACTIVE, dueTodayOnly = true)
        "OVERDUE" -> TaskFilter(status = StatusFilter.ACTIVE, overdueOnly = true)
        "COMPLETED" -> TaskFilter(status = StatusFilter.COMPLETED)
        else -> TaskFilter()
    }

    fun percent(part: Int, total: Int): Int = if (total == 0) 0 else part * 100 / total

    fun stats(tasks: List<Task>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Stats {
        val today = now.toLocalDate(zone)
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val high = tasks.filter { it.priority == Priority.HIGH }
        val highDone = high.count { it.isCompleted }
        return Stats(
            total = total,
            active = total - completed,
            completed = completed,
            highActive = high.size - highDone,
            dueToday = tasks.count { isDueToday(it, today, zone) },
            overdue = tasks.count { isOverdue(it, now) },
            completedToday = tasks.count { it.isCompleted && it.completedAt?.toLocalDate(zone) == today },
            completionPct = percent(completed, total),
            highTotal = high.size,
            highCompleted = highDone,
            highCompletionPct = percent(highDone, high.size)
        )
    }

    /** Completed-task counts for the last 7 days (oldest first, today last). Uses completedAt only. */
    fun weeklyCompletion(tasks: List<Task>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, Int>> {
        val today = now.toLocalDate(zone)
        return (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            d to tasks.count { it.isCompleted && it.completedAt?.toLocalDate(zone) == d }
        }
    }

    fun byPriority(tasks: List<Task>): List<Pair<Priority, Int>> =
        Priority.values().map { p -> p to tasks.count { it.priority == p } }

    fun byCategory(tasks: List<Task>): List<Pair<Category, Int>> =
        Category.values().map { c -> c to tasks.count { it.category == c } }.filter { it.second > 0 }

    /** Today's focus: overdue + due today + high-priority active, de-duplicated, default order. */
    fun todaysFocus(tasks: List<Task>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        val today = now.toLocalDate(zone)
        return tasks.filter { t ->
            !t.isCompleted && (t.priority == Priority.HIGH || isOverdue(t, now) || isDueToday(t, today, zone))
        }.sortedWith(defaultComparator(now))
    }

    /** Next deadlines that are still in the future and not already shown as due today. */
    fun upcoming(tasks: List<Task>, now: Long, limit: Int, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        val today = now.toLocalDate(zone)
        return tasks.filter { !it.isCompleted && it.dueAt != null && it.dueAt >= now && !isDueToday(it, today, zone) }
            .sortedBy { it.dueAt }.take(limit)
    }

    fun greeting(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    /** Returns an error message or null when valid. */
    fun validate(
        title: String, startAt: Long?, dueAt: Long?, durationMin: Int?, recurrence: Recurrence
    ): String? {
        if (title.isBlank()) return "Title is required"
        if (title.trim().length > 100) return "Title must be 100 characters or fewer"
        if (startAt != null && dueAt != null && dueAt < startAt) return "Deadline cannot be earlier than the start time"
        if (durationMin != null && (durationMin <= 0 || durationMin > MAX_DURATION_MIN)) return "Duration must be 1 to $MAX_DURATION_MIN minutes"
        if (recurrence != Recurrence.NONE && startAt == null && dueAt == null) return "Repeating tasks need a start time or deadline"
        return null
    }

    /**
     * Next occurrence of a recurring task, based on the deadline (else start time). Returns null when
     * the task does not repeat or has no date to advance from. The copy has an empty id.
     */
    fun nextOccurrence(t: Task, zone: ZoneId = ZoneId.systemDefault()): Task? {
        if (t.recurrence == Recurrence.NONE) return null
        val anchor = t.dueAt ?: t.startAt ?: return null
        val aDate = anchor.toLocalDate(zone)
        val next: LocalDate = when (t.recurrence) {
            Recurrence.DAILY -> aDate.plusDays(1)
            Recurrence.WEEKLY -> aDate.plusWeeks(1)
            Recurrence.MONTHLY -> aDate.plusMonths(1)
            Recurrence.NONE -> return null
        }
        val days = java.time.temporal.ChronoUnit.DAYS.between(aDate, next)
        fun sh(ms: Long?): Long? = ms?.let { Instant.ofEpochMilli(it).atZone(zone).plusDays(days).toInstant().toEpochMilli() }
        return t.copy(
            id = "", isCompleted = false, completedAt = null, recurrenceSpawned = false,
            startAt = sh(t.startAt), dueAt = sh(t.dueAt)
        )
    }
}
