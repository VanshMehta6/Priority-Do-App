# PriorityDo - Sun Edition

Task, priority and time-management app for Android.
**Developer:** Vansh Mehta | **Application ID:** `com.example.vanshmehta.prioritydo`

PriorityDo lets you create, prioritise, schedule, complete and analyse tasks. It has a sun-inspired
Material 3 design, a focus countdown timer, live insights, and per-user cloud sync through Firebase.

> **Status:** the source was written to the PRD but has **not been compiled, run or tested** in the
> environment that produced it (no Android SDK / Gradle available there). Build it in Android Studio and
> work through `TESTING_CHECKLIST.md` before relying on it. Fix any dependency-version or compile
> issues Android Studio reports.

## 1. Features

- Email/password registration, login, password reset, persistent session, logout with confirmation
- Task CRUD with High / Medium / Low **priority** kept separate from Critical / Important / Normal **importance**
- Categories: Academic, Work and Career, Personal, Health, Finance, Household, Other
- Optional start time, deadline and estimated duration (date/time pickers appear only when you tap a field)
- Default list order: High, Medium, Low; inside a priority: overdue first, earliest deadline, earliest start, undated, then creation time
- Live search (title + description), composable filters (All / Active / Completed, High / Medium / Low, Overdue, Due today, Category), five sort modes
- Active and Completed sections, swipe right to complete/reopen, swipe left to delete (with Undo)
- Dashboard: live stats, completion ring, Today's focus, upcoming deadlines, quick actions, first-task empty state
- Focus timer: select duration, start, pause, resume, reset, optional linked task
- Insights from real data: completion ring, completed vs active, priority and category distribution, 7-day completion trend
- Profile: edit display name, theme (System / Light / Dark, persisted with DataStore), app version, about
- Light and dark themes, animated check marks, skeleton placeholders, animated empty states, subtle celebration when every task is done

**Deliberately not included:** any Calendar screen/route/view, and any task reminder feature
(no reminder fields, schedulers, receivers or deadline notifications).

## 2. Technology stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM + Repository, Coroutines, Flow / StateFlow,
`collectAsStateWithLifecycle()`, Firebase Authentication, Firebase Realtime Database, Jetpack DataStore,
Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`), JUnit 4.

## 3. Architecture and folders

```
VanshMehta-PriorityDo/
├── app/src/main/java/com/example/vanshmehta/prioritydo/
│   ├── MainActivity.kt, PriorityDoApplication.kt
│   ├── data/
│   │   ├── model/        Task, UserProfile, enums
│   │   ├── repository/   AuthRepository, TaskRepository (all Firebase access)
│   │   └── prefs/        SettingsStore (DataStore: theme)
│   ├── viewmodel/        AuthViewModel, TaskViewModel, TimerViewModel
│   ├── ui/
│   │   ├── theme/        Sun palette, typography, shapes
│   │   ├── navigation/   Routes, bottom bar, auth routing
│   │   ├── components/   TaskCard, badges, dialogs, ProgressRing, skeletons
│   │   └── screens/      auth, home, tasks, timer, insights, profile
│   └── util/             TaskLogic (sort/filter/stats/validation), DateFmt, TimerNotifier, Permissions
├── app/src/test/         TaskLogicTest (sorting, filters, stats, validation, recurrence)
├── database.rules.json   Realtime Database security rules
├── TESTING_CHECKLIST.md
└── README.md
```

Repositories own Firebase; ViewModels expose `StateFlow`; screens render state and forward events.
Realtime listeners are wrapped in `callbackFlow` and removed when the collecting coroutine is cancelled
(`TaskViewModel.setUser` cancels old jobs on user change, so there are no duplicate listeners).

## 4. Requirements

- Android Studio Ladybug (2024.2) or newer, JDK 17 (the bundled JetBrains Runtime is fine)
- Android SDK 35, minSdk 26
- A Google/Firebase account

## 5. Firebase setup

1. Open the [Firebase console](https://console.firebase.google.com), **Add project**.
2. **Add app -> Android**. Package name: `com.example.vanshmehta.prioritydo`. Register the app.
3. Download `google-services.json` and place it at `app/google-services.json`.
   It is git-ignored; never commit it. `app/google-services.json.example` shows the expected shape only.
4. **Build -> Authentication -> Sign-in method**: enable **Email/Password**.
5. **Build -> Realtime Database -> Create database** (pick a location, start in *locked mode*).
6. Open the database **Rules** tab, paste the contents of `database.rules.json`, **Publish**.

### Security rules summary

- Everything is denied by default.
- `/users/{uid}` and `/tasks/{uid}` are readable and writable only when `auth.uid == {uid}`.
- Each task must have `title`, `priority`, `importance`, `category`, `isCompleted`, `createdAt`, `ownerUid`;
  `ownerUid` must equal the signed-in user.
- Values are validated (priority/importance/category/recurrence whitelists, string lengths, numeric types,
  duration 1-1440, deadline not before start). Unknown fields are rejected.

## 6. Data model

```
/users/{uid}            displayName, email, createdAt
/tasks/{uid}/{taskId}   title, description, priority, importance, category, isCompleted,
                        startAt, dueAt, durationMinutes, recurrence, recurrenceSpawned,
                        notes, createdAt, updatedAt, completedAt, ownerUid
```

- All dates are Unix epoch **milliseconds** (UTC). Missing optional values are simply absent.
- Task IDs come from `push().key`.
- `completedAt` is set when a task is completed and removed when it is reopened. The weekly trend uses only this field.
- Enums are stored by name (`HIGH`, `WORK_CAREER`, ...).

## 7. Build and run

1. Open the folder in Android Studio and let Gradle sync (requires `app/google-services.json`).
2. Pick an emulator or device (API 26+) and press **Run**.
3. Command line: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 8. Recurring tasks

Model: **create the next occurrence when the current one is completed.**
Daily/Weekly/Monthly advance the deadline (or start time if there is no deadline) by one day/week/month
(month-end is clamped, e.g. 31 Jan -> 28 Feb). A recurring task requires a deadline or start time.
The completed task and the new one are written in a single atomic `updateChildren`, and the completed task
is flagged `recurrenceSpawned = true`, so app restarts, listener updates, or reopening and re-completing
never create a second copy.

## 9. Focus timer behaviour and limits

- Remaining time = end time minus `SystemClock.elapsedRealtime()` (monotonic), recomputed every 200 ms. It does not drift.
- One ticker job only; pause/resume/reset cancel it. The timer lives in an activity-scoped ViewModel, so it keeps
  running while you move between screens or background the app (as long as Android keeps the process alive).
- **Process death:** if Android kills the process, the session is discarded and the timer resets on next launch. Nothing is persisted.
- **Completion notification (optional):** shown only for a session you started, when it reaches zero while the process is alive,
  and only if notifications are allowed (Android 13+ asks when you tap Start). It is not scheduled, so if the process is
  killed or frozen before the end, no notification appears. It is not a task reminder.

## 10. Offline behaviour

Realtime Database disk persistence is enabled (`PriorityDoApplication`) and `keepSynced(true)` is set on the
user's task node. Data already cached is shown offline, and writes made offline are queued and sent on reconnect.
When a save does not get a server acknowledgement within 5 seconds the app says it was saved on this device and will sync later.
A banner is shown while disconnected. Not verified: conflict behaviour when the same task is edited on two offline devices
(Firebase last-write-wins per field). First launch with no cache and no network shows an empty list.

## 11. Testing

- Unit tests: `./gradlew testDebugUnitTest` (sorting, filters, search, stats, zero-task case, weekly trend, validation, recurrence).
- Manual: follow `TESTING_CHECKLIST.md`.

## 12. Troubleshooting

| Problem | Fix |
|---|---|
| `File google-services.json is missing` | Place the file in `app/` (step 5.3). |
| Package mismatch error from the google-services plugin | The app registered in Firebase must use `com.example.vanshmehta.prioritydo`. |
| `PERMISSION_DENIED` / "Permission denied" | Rules not published, or you are signed out. Publish `database.rules.json`. |
| Writes rejected after editing rules | A field name or value does not match the validation rules. |
| Registration says email exists | Use Login or Forgot password. |
| Gradle sync fails on versions | Use the Android Studio version in section 4 or update `gradle/libs.versions.toml` to a mutually compatible set. |
| Data does not appear offline on first run | Persistence only has data that was loaded online at least once. |

## 13. Known limitations

- Not built or run by the author of this source package (see Status).
- Swipe-to-delete offers Undo from memory (this session only); the Delete buttons ask for confirmation first.
- Recurring tasks only spawn the next occurrence when completed; missed occurrences are not backfilled.
- The completion notification can be missed if the process is killed.
- No automated UI/instrumentation tests are included.
