package com.growthos.personal.reminders

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

class HealthIssue(val text: String, val fixLabel: String, val fix: () -> Unit)

/** Finds the phone settings that commonly stop alarms and reminders, with a shortcut to fix each. */
object AlarmHealth {

    fun issues(context: Context): List<HealthIssue> {
        val list = mutableListOf<HealthIssue>()
        val pkg = context.packageName

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            list.add(
                HealthIssue("Notifications are off", "Allow") {
                    open(
                        context,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                    )
                }
            )
        }
        if (!ReminderScheduler.canScheduleExact(context)) {
            list.add(
                HealthIssue("Exact alarms are not allowed", "Allow") {
                    open(
                        context,
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg"))
                    )
                }
            )
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            list.add(
                HealthIssue("Battery saver can stop alarms", "Fix") {
                    open(
                        context,
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))
                    )
                }
            )
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                list.add(
                    HealthIssue("Alarm screen cannot open over the lock screen", "Allow") {
                        open(
                            context,
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg"))
                        )
                    }
                )
            }
        }
        return list
    }

    fun isXiaomiFamily(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m == "xiaomi" || m == "redmi" || m == "poco"
    }

    fun openAppSettings(context: Context) {
        open(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
        )
    }

    private fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                // nothing else to open
            }
        }
    }
}
