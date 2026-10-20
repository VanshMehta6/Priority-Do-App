# Manual testing checklist - PriorityDo

Mark each item after testing on a device/emulator. Use two accounts (A and B) where noted.

## Authentication
- [ ] Register with name, email, password, confirm; lands on Home with greeting
- [ ] Empty/invalid fields and mismatched passwords show clear errors
- [ ] Password show/hide works on every password field
- [ ] Login succeeds with valid credentials; wrong password shows "Invalid email or password."
- [ ] Airplane mode login shows the network message
- [ ] Forgot password sends an email (check inbox); invalid email is rejected
- [ ] Force-stop and relaunch: still signed in, goes straight to Home
- [ ] Sign out (confirmation) returns to Login; no previous tasks visible afterwards

## Tasks
- [ ] Create a task with only the required fields
- [ ] Create a task with every optional field
- [ ] View it in Task Details (absent optional fields show "Not set")
- [ ] Edit it; existing data preserved; changes persist after restart
- [ ] Save button shows a spinner and cannot be double-tapped
- [ ] Delete from card (confirmation), from details (confirmation), by swipe (Undo works)
- [ ] Complete and reopen from checkbox, swipe-right and details; stats update at once
- [ ] A task with a past deadline is NOT auto-completed
- [ ] Search by title and by description (notes are not searched)
- [ ] Filters: All, Active, Completed, High, Medium, Low, Overdue, Due today, Category; combine two filters
- [ ] Default order High > Medium > Low; overdue first inside a priority
- [ ] Sort: Priority, Deadline, Recently created, Oldest first, Alphabetical
- [ ] Deadline earlier than start is blocked; duration 0 and 1441 are blocked
- [ ] Overdue label appears on late active tasks only
- [ ] Empty states: no tasks, no filter matches
- [ ] Recurring task: completing creates exactly one next task; reopen + complete again creates none; restart creates none

## Dashboard
- [ ] Stats (active, completed, high priority, due today, overdue, completion %) match the task list
- [ ] Today's focus and upcoming deadlines contain only real dated tasks
- [ ] "Create Your First Task" appears with zero tasks
- [ ] Celebration card appears when all tasks are complete

## Focus timer
- [ ] Start, pause, resume, reset
- [ ] Preset and custom durations; invalid custom values ignored
- [ ] Open from Task Details: uses estimated duration when set
- [ ] Keeps running while navigating to other screens
- [ ] Lock screen / background for 30 s then return: remaining time is correct
- [ ] Completion state shown; notification appears if allowed
- [ ] Swipe app away mid-session: timer is reset on next launch (documented)

## Insights
- [ ] Totals, active, overdue, completed today match the list
- [ ] Completion % = completed / total; zero tasks shows the empty state / 0%
- [ ] Priority and category breakdowns match
- [ ] 7-day chart: complete a task -> today's bar increments; reopen -> decrements

## Firebase and security
- [ ] Change on device 1 appears on device 2 within seconds
- [ ] User B cannot see user A's tasks
- [ ] Rules simulator: B reading `/tasks/{A uid}` is denied; unauthenticated read is denied
- [ ] Offline: add a task, see "saved on this device"; reconnect -> appears in the console
- [ ] Debug: leaving the Tasks screen / logging out does not leave extra listeners

## Profile and settings
- [ ] Edit display name; greeting and profile update
- [ ] Theme System / Light / Dark applies immediately and survives restart
- [ ] App version and About dialog show

## Scope verification
- [ ] No Calendar tab/screen/route anywhere
- [ ] No reminder switches, fields, settings or deadline notifications
- [ ] Date/time pickers open only from the Add/Edit task form
- [ ] Timer notification only follows a session you started

## Accessibility and layout
- [ ] Font scale 200%: no clipped text
- [ ] TalkBack reads icons and the completion checkbox
- [ ] Rotation / small phone / tablet layouts usable
- [ ] Keyboard does not hide form fields
