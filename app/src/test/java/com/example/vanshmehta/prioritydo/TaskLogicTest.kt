package com.example.vanshmehta.prioritydo

import com.example.vanshmehta.prioritydo.data.model.Category
import com.example.vanshmehta.prioritydo.data.model.Priority
import com.example.vanshmehta.prioritydo.data.model.Recurrence
import com.example.vanshmehta.prioritydo.data.model.Task
import com.example.vanshmehta.prioritydo.util.SortMode
import com.example.vanshmehta.prioritydo.util.StatusFilter
import com.example.vanshmehta.prioritydo.util.TaskFilter
import com.example.vanshmehta.prioritydo.util.TaskLogic
import com.example.vanshmehta.prioritydo.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskLogicTest {
    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 10, 19)
    private val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L
    private val dayMs = 24 * hour

    private fun t(
        id: String, p: Priority = Priority.MEDIUM, due: Long? = null, start: Long? = null, created: Long = 0,
        done: Boolean = false, completedAt: Long? = null, cat: Category = Category.OTHER,
        title: String = "Task $id", desc: String = "", notes: String = "", rec: Recurrence = Recurrence.NONE
    ) = Task(
        id = id, title = title, description = desc, notes = notes, priority = p, dueAt = due, startAt = start,
        createdAt = created, isCompleted = done, completedAt = completedAt, category = cat, recurrence = rec
    )

    // ---------- sorting ----------

    @Test fun priorityOrderIsHighMediumLow() {
        val sorted = TaskLogic.sort(listOf(t("l", Priority.LOW), t("h", Priority.HIGH), t("m", Priority.MEDIUM)), SortMode.PRIORITY, now)
        assertEquals(listOf("h", "m", "l"), sorted.map { it.id })
    }

    @Test fun overdueFirstInsideSamePriority() {
        val sorted = TaskLogic.sort(listOf(t("future", Priority.HIGH, due = now + hour), t("late", Priority.HIGH, due = now - hour)), SortMode.PRIORITY, now)
        assertEquals("late", sorted.first().id)
    }

    @Test fun overdueDoesNotJumpAbovePriority() {
        val sorted = TaskLogic.sort(listOf(t("lowLate", Priority.LOW, due = now - hour), t("highFuture", Priority.HIGH, due = now + hour)), SortMode.PRIORITY, now)
        assertEquals("highFuture", sorted.first().id)
    }

    @Test fun earlierDeadlineFirstAndUndatedLast() {
        val sorted = TaskLogic.sort(listOf(t("none"), t("later", due = now + 5 * hour), t("soon", due = now + hour)), SortMode.PRIORITY, now)
        assertEquals(listOf("soon", "later", "none"), sorted.map { it.id })
    }

    @Test fun startTimeBreaksTieWhenNoDeadline() {
        val sorted = TaskLogic.sort(listOf(t("b", created = 2), t("a", created = 1), t("s", start = now + hour)), SortMode.PRIORITY, now)
        assertEquals(listOf("s", "a", "b"), sorted.map { it.id })
    }

    @Test fun deadlineBeatsStartTimeOnlyTask() {
        val sorted = TaskLogic.sort(listOf(t("start", start = now + hour), t("due", due = now + 9 * hour)), SortMode.PRIORITY, now)
        assertEquals("due", sorted.first().id)
    }

    @Test fun deadlineSortPutsNullsLast() {
        val sorted = TaskLogic.sort(listOf(t("n"), t("d", due = now)), SortMode.DEADLINE, now)
        assertEquals("d", sorted.first().id)
    }

    @Test fun createdSorts() {
        val list = listOf(t("old", created = 1), t("new", created = 9))
        assertEquals("new", TaskLogic.sort(list, SortMode.NEWEST, now).first().id)
        assertEquals("old", TaskLogic.sort(list, SortMode.OLDEST, now).first().id)
    }

    @Test fun alphaSortIgnoresCase() {
        val sorted = TaskLogic.sort(listOf(t("b", title = "banana"), t("a", title = "Apple")), SortMode.ALPHA, now)
        assertEquals("a", sorted.first().id)
    }

    @Test fun sortingDoesNotChangeTaskData() {
        val original = listOf(t("l", Priority.LOW, due = now), t("h", Priority.HIGH, due = now + hour))
        val sorted = TaskLogic.sort(original, SortMode.ALPHA, now)
        assertEquals(Priority.LOW, sorted.first { it.id == "l" }.priority)
        assertEquals(now, sorted.first { it.id == "l" }.dueAt)
    }

    // ---------- overdue / due today ----------

    @Test fun overdueRules() {
        assertTrue(TaskLogic.isOverdue(t("x", due = now - 1), now))
        assertFalse(TaskLogic.isOverdue(t("x", due = now + 1), now))
        assertFalse(TaskLogic.isOverdue(t("x", due = now - 1, done = true), now))
        assertFalse(TaskLogic.isOverdue(t("x"), now))
    }

    @Test fun dueTodayUsesDeadlineOnly() {
        assertTrue(TaskLogic.isDueToday(t("x", due = now + hour), day, zone))
        assertFalse(TaskLogic.isDueToday(t("x", start = now + hour), day, zone))
        assertFalse(TaskLogic.isDueToday(t("x", due = now + dayMs), day, zone))
        assertFalse(TaskLogic.isDueToday(t("x", due = now + hour, done = true), day, zone))
    }

    // ---------- filters ----------

    private val sample = listOf(
        t("a", Priority.HIGH, due = now - hour, cat = Category.WORK_CAREER, title = "Report", desc = "quarterly numbers", notes = "secret-note"),
        t("b", Priority.LOW, due = now + hour, cat = Category.PERSONAL, title = "Call mom"),
        t("c", Priority.MEDIUM, done = true, completedAt = now, cat = Category.PERSONAL, title = "Buy milk"),
        t("d", Priority.HIGH, cat = Category.HEALTH, title = "Gym")
    )

    private fun ids(f: TaskFilter) = TaskLogic.applyFilter(sample, f, now, zone).map { it.id }.toSet()

    @Test fun statusFilters() {
        assertEquals(setOf("a", "b", "c", "d"), ids(TaskFilter(status = StatusFilter.ALL)))
        assertEquals(setOf("a", "b", "d"), ids(TaskFilter(status = StatusFilter.ACTIVE)))
        assertEquals(setOf("c"), ids(TaskFilter(status = StatusFilter.COMPLETED)))
    }

    @Test fun priorityAndCategoryFilters() {
        assertEquals(setOf("a", "d"), ids(TaskFilter(priority = Priority.HIGH)))
        assertEquals(setOf("b", "c"), ids(TaskFilter(category = Category.PERSONAL)))
    }

    @Test fun filtersCompose() {
        assertEquals(setOf("c"), ids(TaskFilter(category = Category.PERSONAL, status = StatusFilter.COMPLETED)))
        assertEquals(setOf("a"), ids(TaskFilter(priority = Priority.HIGH, overdueOnly = true)))
    }

    @Test fun overdueAndDueTodayFilters() {
        assertEquals(setOf("a"), ids(TaskFilter(overdueOnly = true)))
        assertEquals(setOf("a", "b"), ids(TaskFilter(dueTodayOnly = true)))
    }

    @Test fun searchMatchesTitleAndDescriptionButNotNotes() {
        assertEquals(setOf("b"), ids(TaskFilter(query = "MOM")))
        assertEquals(setOf("a"), ids(TaskFilter(query = "quarterly")))
        assertEquals(emptySet<String>(), ids(TaskFilter(query = "secret-note")))
    }

    @Test fun filteredResultIsSortedByPriority() {
        val out = TaskLogic.applyFilter(sample, TaskFilter(status = StatusFilter.ACTIVE), now, zone).map { it.id }
        assertEquals(listOf("a", "d", "b"), out)
    }

    @Test fun sectionsSeparateCompleted() {
        val (active, done) = TaskLogic.sections(TaskLogic.applyFilter(sample, TaskFilter(), now, zone))
        assertEquals(setOf("a", "b", "d"), active.map { it.id }.toSet())
        assertEquals(listOf("c"), done.map { it.id })
    }

    // ---------- stats ----------

    @Test fun statsCounts() {
        val s = TaskLogic.stats(sample, now, zone)
        assertEquals(4, s.total)
        assertEquals(1, s.completed)
        assertEquals(3, s.active)
        assertEquals(2, s.highActive)
        assertEquals(2, s.dueToday)
        assertEquals(1, s.overdue)
        assertEquals(1, s.completedToday)
        assertEquals(25, s.completionPct)
    }

    @Test fun zeroTasksGivesZeroPercent() {
        val s = TaskLogic.stats(emptyList(), now, zone)
        assertEquals(0, s.completionPct)
        assertEquals(0, s.highCompletionPct)
        assertEquals(0, TaskLogic.percent(0, 0))
    }

    @Test fun highPriorityCompletionRate() {
        val list = listOf(
            t("1", Priority.HIGH, done = true, completedAt = now),
            t("2", Priority.HIGH),
            t("3", Priority.LOW, done = true, completedAt = now)
        )
        val s = TaskLogic.stats(list, now, zone)
        assertEquals(50, s.highCompletionPct)
        assertEquals(1, s.highActive)
    }

    @Test fun weeklyCompletionUsesCompletedAtNotCreatedAt() {
        val list = listOf(
            t("today", done = true, completedAt = now, created = now - 5 * dayMs),
            t("twoDaysAgo", done = true, completedAt = now - 2 * dayMs),
            t("tooOld", done = true, completedAt = now - 10 * dayMs),
            t("noTimestamp", done = true, completedAt = null, created = now),
            t("reopened", done = false, completedAt = now)
        )
        val w = TaskLogic.weeklyCompletion(list, now, zone)
        assertEquals(7, w.size)
        assertEquals(day, w.last().first)
        assertEquals(1, w.last().second)
        assertEquals(1, w[4].second)
        assertEquals(2, w.sumOf { it.second })
    }

    @Test fun priorityAndCategoryBreakdowns() {
        assertEquals(listOf(2, 1, 1), TaskLogic.byPriority(sample).map { it.second })
        assertEquals(setOf(Category.WORK_CAREER, Category.PERSONAL, Category.HEALTH), TaskLogic.byCategory(sample).map { it.first }.toSet())
        assertEquals(2, TaskLogic.byCategory(sample).first { it.first == Category.PERSONAL }.second)
    }

    // ---------- dashboard helpers ----------

    @Test fun todaysFocusIncludesHighOverdueAndDueToday() {
        val focus = TaskLogic.todaysFocus(sample, now, zone).map { it.id }
        assertEquals(listOf("a", "d", "b"), focus)
        assertFalse("c" in focus)
    }

    @Test fun upcomingOnlyFutureDeadlinesNotToday() {
        val list = listOf(t("tomorrow", due = now + dayMs), t("later", due = now + 3 * dayMs), t("today", due = now + hour), t("past", due = now - dayMs))
        assertEquals(listOf("tomorrow", "later"), TaskLogic.upcoming(list, now, 5, zone).map { it.id })
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", TaskLogic.greeting(8))
        assertEquals("Good afternoon", TaskLogic.greeting(13))
        assertEquals("Good evening", TaskLogic.greeting(20))
    }

    // ---------- validation ----------

    @Test fun titleRequired() {
        assertNotNull(TaskLogic.validate("   ", null, null, null, Recurrence.NONE))
        assertNotNull(TaskLogic.validate("x".repeat(101), null, null, null, Recurrence.NONE))
        assertNull(TaskLogic.validate("ok", null, null, null, Recurrence.NONE))
    }

    @Test fun deadlineCannotBeBeforeStart() {
        assertNotNull(TaskLogic.validate("t", now, now - 1, null, Recurrence.NONE))
        assertNull(TaskLogic.validate("t", now, now, null, Recurrence.NONE))
        assertNull(TaskLogic.validate("t", null, now, null, Recurrence.NONE))
    }

    @Test fun durationBounds() {
        assertNotNull(TaskLogic.validate("t", null, null, 0, Recurrence.NONE))
        assertNotNull(TaskLogic.validate("t", null, null, -5, Recurrence.NONE))
        assertNotNull(TaskLogic.validate("t", null, null, TaskLogic.MAX_DURATION_MIN + 1, Recurrence.NONE))
        assertNull(TaskLogic.validate("t", null, null, 1, Recurrence.NONE))
        assertNull(TaskLogic.validate("t", null, null, TaskLogic.MAX_DURATION_MIN, Recurrence.NONE))
    }

    @Test fun recurrenceNeedsADate() {
        assertNotNull(TaskLogic.validate("t", null, null, null, Recurrence.DAILY))
        assertNull(TaskLogic.validate("t", null, now, null, Recurrence.DAILY))
    }

    @Test fun emailAndPasswordValidators() {
        assertTrue(Validators.isEmail("a@b.co"))
        assertFalse(Validators.isEmail("nope"))
        assertNotNull(Validators.passwordError("12345"))
        assertNull(Validators.passwordError("123456"))
    }

    // ---------- recurrence ----------

    @Test fun nextOccurrenceDailyWeeklyMonthly() {
        val base = t("r", due = now, start = now - hour, rec = Recurrence.DAILY)
        assertEquals(now + dayMs, TaskLogic.nextOccurrence(base, zone)!!.dueAt)
        assertEquals(now - hour + dayMs, TaskLogic.nextOccurrence(base, zone)!!.startAt)
        assertEquals(now + 7 * dayMs, TaskLogic.nextOccurrence(base.copy(recurrence = Recurrence.WEEKLY), zone)!!.dueAt)
        assertEquals(now + 31 * dayMs, TaskLogic.nextOccurrence(base.copy(recurrence = Recurrence.MONTHLY), zone)!!.dueAt)
    }

    @Test fun nextOccurrenceIsFreshAndActive() {
        val done = t("r", due = now, done = true, completedAt = now, rec = Recurrence.DAILY).copy(recurrenceSpawned = true)
        val next = TaskLogic.nextOccurrence(done, zone)!!
        assertEquals("", next.id)
        assertFalse(next.isCompleted)
        assertNull(next.completedAt)
        assertFalse(next.recurrenceSpawned)
        assertEquals(Recurrence.DAILY, next.recurrence)
    }

    @Test fun noNextOccurrenceWithoutRecurrenceOrDate() {
        assertNull(TaskLogic.nextOccurrence(t("x", due = now), zone))
        assertNull(TaskLogic.nextOccurrence(t("x", rec = Recurrence.DAILY), zone))
    }

    @Test fun monthlyClampsToShorterMonths() {
        val jan31 = LocalDate.of(2027, 1, 31).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val next = TaskLogic.nextOccurrence(t("m", due = jan31, rec = Recurrence.MONTHLY), zone)!!
        assertEquals(LocalDate.of(2027, 2, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), next.dueAt)
    }
}
