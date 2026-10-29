# Growth OS - HANDOFF (attach this + latest repo zip in a NEW chat)

Reply in simple Hinglish, SHORT. I'm Gourav, complete beginner, Android phone only, zero budget, Claude Free (limits hit fast): do ONE feature group per zip, no long explanations.
Give code as growth-os-project.zip with ONLY changed files at real paths (app/src/main/java/com/growthos/personal/..., app/build.gradle.kts, HANDOFF.md). Never ask me for passwords/keys in chat.

## Project
Growth OS = private native Android app (Kotlin, Jetpack Compose, Room), package com.growthos.personal, repo github.com/gaurav-in33/growth-os (public).
Current: v1.12.0, DB v9. A-D verified. E (Onboarding, v1.9.0) and F (Clear All Data, v1.10.0) G (AI, v1.11.0) and H (widget, v1.12.0) built, NOT yet verified.

## How updates work (do not change)
1. You give growth-os-project.zip (only changed files).
2. I upload it at repo root (Add file > Upload files > Commit).
3. I run github.com/gaurav-in33/growth-os/actions/workflows/build.yml > Run workflow (never "Re-run"). It unzips, commits, builds debug APK.
4. If red, I send the error text of the failed step.
First step in a new chat: ask me to attach latest repo zip (GitHub > Code > Download ZIP), unzip, read the files you will change. Bump versionCode/versionName in app/build.gradle.kts every zip. No Kotlin compiler in sandbox: keep code simple, copy API patterns from existing files, check imports/brackets. Each DB change = new Room version + Migration (ALTER TABLE with DEFAULTs matching @ColumnInfo(defaultValue)). Backup restore only works for same DB version (tell me to make a new backup after each DB bump).

## Design rules
Orange glass look: GlassCard, GlowButton, SoftButton, Segmented, TileLabel, RingProgress, BarChart, MoreMenu, CheckDot, MiniChip, TagPill, ThinBar, TextEditDialog (ui/Components.kt, ui/GrowthTheme.kt, ui/EditParts.kt). Clean UI, no filler text, nothing under the nav.
Hubs (ui/Hubs.kt): Plan = Routine|Tasks|Calendar, Track = Habits|Health|Fitness(Log|Lifts|Plan|Body), Grow = Goals|Learn(Courses|Roadmap|Log)|Projects, More = Report/Money/Notes/Settings (ProfileScreen.kt).

## Already built (v1.8.0)
Tasks planner (data/TaskEntity, TaskDao, TaskRules; ui/TasksTab; reminders with Mark complete/Snooze in ReminderScheduler + AlarmReceiver). Routine ticks + templates + pause today (data/RoutineItem, RoutineTemplates; PlanScreen.kt = RoutineSection). Score weights: tasks 20, habits 20, routine 15, focus 15, water 10, study 10, workout 10 (ui/DayStats.kt). Learning/fitness/project milestones/sleep times (data/LearnFitnessData.kt). Goals with deadline, milestones, behind-schedule advice, completed history (data/Goal.kt GoalRules, GoalsSection in ui/GrowScreen.kt). Also: wake alarm (AlarmService/AlarmActivity), habits+streaks, finance, notes, XP/level, daily review, weekly report, backup/restore, theme.

## E. Onboarding (built v1.9.0: 4 steps name/goals/routine/notifications, all skippable; verify on fresh install)
Steps: name -> pick goals (chips + custom, creates Weekly/Long-term goals via GoalEntity) -> review default routine (seed routine in MainActivity using RoutineTemplates.MY_DAY, not only in Plan; keep settings.routineSeeded flag) -> explain reminders + button "Allow notifications" (POST_NOTIFICATIONS). All steps skippable. Existing ui/OnboardingScreen.kt + MainActivity (ProfileStore.isOnboardingDone) are the places to edit.

## Later (not now), in this order
F DONE v1.10.0 (More > Settings > Danger zone, type DELETE, cancels alarms, wipes DB+prefs, restarts app).
G DONE v1.11.0 (More > Settings > AI assistant, AiStore/AiClient/AiCard, generateContent, default model gemini-3.8-flash editable, INTERNET permission added). Original spec: Optional AI assistant (own free Gemini key in Settings, default OFF, adds INTERNET permission, never send finance/notes).
H DONE v1.12.0 (TodayWidget RemoteViews: tasks due/overdue, habits done; refreshes on app pause, every 30 min, after Clear All). Original: Home-screen widget.
Then: (NEXT) release APK + signing (key only in GitHub Secrets), accessibility, unit tests.

## Known small issues
XP drops 5 when a recurring task rolls over (XP counts done tasks). Rest day gives 0 workout points. Opening a task reminder lands on Plan > Routine (not Tasks).

## Notes
Alarm: AlarmService plays ringtone on ALARM stream, 5 min auto stop; Plan > Routine > Alarm check has Fix buttons and Test alarm/Test reminder. Phone looks like Xiaomi: Autostart ON + Battery no restrictions needed.
Name: ProfileStore saved name is edited in More > Settings.
