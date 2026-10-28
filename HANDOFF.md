# Growth OS - HANDOFF (paste/attach this in a NEW chat)

Reply in simple Hinglish. I'm Gourav (not "German"), complete beginner, Android phone only, zero budget, Claude Free (limits hit fast, so keep replies short and do ONE feature group per zip).
Give code as a zip of only the changed files. Never ask me for passwords/keys in chat.

## Project
Growth OS = private native Android app (Kotlin, Jetpack Compose, Room), package com.growthos.personal, repo github.com/gaurav-in33/growth-os (public).
Current version: v1.6.0 (B built, not yet verified on phone; A verified in v1.5.0), DB v7. Everything below is built, green and working on my phone:
glass design (Today, Plan, Track, Grow, More), liquid nav, real wake alarm (AlarmService + AlarmActivity + Alarm check/Test buttons), routine reminders, habits+streaks, goals, study/coding logs, projects (progress only), fitness logs, sleep/mood/energy, finance, notes, score+XP+level, daily review, weekly report, backup/restore, theme choice.

## How updates work (do not change)
1. You give growth-os-project.zip containing ONLY changed files at their real paths (app/src/main/java/com/growthos/personal/..., app/build.gradle.kts, HANDOFF.md).
2. I upload it at the repo root (Add file > Upload files > Commit).
3. I run github.com/gaurav-in33/growth-os/actions/workflows/build.yml > Run workflow (never "Re-run"). The workflow unzips it into folders, commits, builds the debug APK.
4. If red, I send the error text of the failed step.
Always ask me to attach the latest repo zip (GitHub > Code > Download ZIP) and read it before editing. Bump versionCode/versionName in app/build.gradle.kts each zip. No Kotlin compiler in the sandbox: keep code simple, copy API patterns from existing files, check imports/brackets.

## Design rules (approved)
Orange glass look: use GlassCard, GlowButton, SoftButton, Segmented, TileLabel, RingProgress, BarChart, MoreMenu, CheckDot, TextEditDialog from ui/Components.kt and ui/GrowthTheme.kt. Clean UI, no filler sentences, nothing drawn under the nav. Hubs: Track=Habits/Health/Fitness, Grow=Goals/Learn/Projects, More=Report/Money/Notes/Settings (Segmented in ui/Hubs.kt, ProfileScreen.kt). Plan is a single screen now.

## TODO: A is DONE in v1.5.0 (files: data/TaskEntity, TaskDao, TaskRules, ui/TasksTab; Plan hub in ui/Hubs.kt; old PlanScreen is now RoutineSection in PlanScreen.kt; task reminders in ReminderScheduler/AlarmReceiver). Next: B C D E, one zip each, B first.
### A. Tasks as a real planner (DB v6) - DONE v1.5.0 (known: XP drops 5 when a recurring task rolls over; old v5 backups cannot restore on v6)
TaskEntity new columns: dueEpochDay Long?, dueMinute Int?, priority Int (1 high,2 med,3 low, default 2), estimateMin Int (0=none), category String (''), energy Int (0 none,1 low,2 med,3 high), repeat String ('none','daily','weekly'), remind Boolean, completedEpochDay Long?.
Migration 5->6 = ALTER TABLE tasks ADD COLUMN ... with matching DEFAULTs (use @ColumnInfo(defaultValue=...) on the entity).
Rules: overdue = !isDone && due < today. Recurring task: when done set completedEpochDay=today; on app start roll over (isDone=false, due=next occurrence). Postpone = due+1 day. "Today tasks" for score = completed today + not done and (no due or due<=today).
UI: Plan becomes a hub Segmented [Routine | Tasks | Calendar]. Tasks tab sections Overdue/Today/Upcoming/No date/Completed; task editor dialog (title, date via android.app.DatePickerDialog, time via TimePickerDialog, priority, duration, category, energy, repeat, reminder switch); calendar = month grid with dots, tap a day to list its tasks. Today tasks card sorted by priority/deadline with a short reason ("Overdue", "Due 3:00 PM", "High priority").
Reminders: ReminderScheduler.scheduleTask/cancelTask, receiver actions Mark complete / Snooze 5 min / Open, respect quiet hours and the 10-12 protected block, reschedule in rescheduleAll and after boot.
Also update DayStats (score uses today's tasks) and DailyReviewCard.
### B. Routine - DONE v1.6.0 (DB v7; data/RoutineTemplates.kt, routine_checks in RoutineItem.kt, pausedDay in SettingsStore, score weights tasks20 habits20 routine15 focus15 water10 study10 workout10). Next: C
Table routine_checks(routineId, epochDay) PK both: tick each routine item per day (CheckDot in Plan timeline), Today hero "Routine x/y" uses ticks, score gets a Routine part (tasks 20, habits 20, routine 15, focus 15, water 10, study 10, workout 10).
Block headers Morning/Afternoon/Evening/Bedtime in the timeline. Templates dialog (My day / Light day / Workout day): Add or Replace (confirm). "Pause routine today" banner (SettingsStore pausedDay): no routine reminders, ignored in score.
### C. Learning + fitness
Tables: courses(id,name,deadlineEpochDay?,isArchived), topics(id,courseId,name,status 0/1/2,weak), skills(id,name,status,position), project_items(id,projectId,parentId default 0,title,isDone) for milestones and subtasks (project progress auto from milestones), exercise_logs(id,epochDay,exercise,sets,reps,weightKg), measurements(id,epochDay,kind,value), workout_plan(dayOfWeek PK,name). Rest day = WorkoutLog type "Rest", 0 min.
UI: Learn = courses with deadline pill, topics (tap cycles status, star = weak) + "Review weak areas" card + skill roadmap; Projects = milestones with subtasks; Fitness = planner (week list), sets x reps @ kg log, body measurements; Health sleep = bedtime + wake time pickers + quality 1-5 (DayCheckin new columns bedtimeMinute default -1, wakeMinute default -1, sleepQuality default 0; sleepMinutes computed).
### D. Goals
Tables goal_milestones(id,goalId,title,isDone); GoalEntity new columns deadlineEpochDay Long?, completedEpochDay Long?. Progress auto from milestones when they exist else +-10. "Behind schedule" advice when progress is 15%+ below the linear plan to the deadline. Completed goals history section.
### E. Onboarding
Steps: name -> pick goals (chips + custom, creates Weekly/Long-term goals) -> review default routine (seed routine in MainActivity, not only in Plan) -> explain reminders + button Allow notifications. All skippable.

## Later (not now)
F Clear All Data with strong confirmation (missing! spec requires), G optional AI assistant (own free Gemini key in Settings, default OFF, adds INTERNET permission, never send finance/notes), H home-screen widget, release APK + signing (key only in GitHub Secrets), accessibility, unit tests.

## Notes
Alarm: AlarmService plays ringtone on ALARM stream, 5 min auto stop, Plan > Alarm check has Fix buttons and Test alarm/Test reminder. My phone looks like Xiaomi: Autostart ON + Battery no restrictions needed.
Name: ProfileStore saved name is edited in More > Settings.
