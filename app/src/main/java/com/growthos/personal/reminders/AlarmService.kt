package com.growthos.personal.reminders

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Plays the wake-up alarm the way a real alarm clock does: a foreground service plays the
 * alarm ringtone on the ALARM audio stream (loud even if notification sounds are muted),
 * vibrates, and shows a full-screen "I'm awake" screen. It stops by itself after 5 minutes
 * and then leaves a quiet "Are you awake?" prompt, so it never loops forever.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isTest = false
    private val timeout = Runnable { onTimeout() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isTest = intent?.getBooleanExtra("test", false) ?: false
        startAlarm()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseAll()
        super.onDestroy()
    }

    private fun startAlarm() {
        ringing = true
        ReminderScheduler.ensureChannels(this)
        val notification = buildNotification()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: Exception) {
            try {
                NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
            } catch (e2: Exception) {
                // nothing more we can do for the notification, the sound still plays
            }
        }
        acquireWakeLock()
        raiseVolume()
        startSound()
        startVibration()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, ALARM_TIMEOUT_MS)
    }

    private fun buildNotification(): android.app.Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val awakePi = PendingIntent.getBroadcast(
            this, 11,
            Intent(this, AlarmReceiver::class.java)
                .setAction(ReminderScheduler.ACTION_AWAKE)
                .putExtra("test", isTest),
            flags
        )
        val snoozePi = PendingIntent.getBroadcast(
            this, 12,
            Intent(this, AlarmReceiver::class.java)
                .setAction(ReminderScheduler.ACTION_SNOOZE)
                .putExtra("type", "wake")
                .putExtra("test", isTest),
            flags
        )
        val screenPi = PendingIntent.getActivity(
            this, 30,
            Intent(this, AlarmActivity::class.java)
                .putExtra("test", isTest)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        return NotificationCompat.Builder(this, ReminderScheduler.CH_WAKE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isTest) "Test alarm" else "Wake up!")
            .setContentText("Tap I'm awake when you are up")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(screenPi)
            .setFullScreenIntent(screenPi, true)
            .addAction(0, "I'm awake", awakePi)
            .addAction(0, "Snooze 5 min", snoozePi)
            .build()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "growthos:alarm")
            lock.acquire(ALARM_TIMEOUT_MS + 60_000L)
            wakeLock = lock
        } catch (e: Exception) {
            // optional, the alarm still works without it
        }
    }

    // A silent alarm is useless, so lift a very low alarm volume to a comfortable level.
    private fun raiseVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < max * 0.5) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, (max * 0.7).toInt().coerceAtLeast(1), 0)
            }
        } catch (e: Exception) {
            // some phones block this in Do Not Disturb, ignore
        }
    }

    private fun startSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            try {
                val r = RingtoneManager.getRingtone(this, uri)
                r.audioAttributes = attrs
                if (Build.VERSION.SDK_INT >= 28) r.isLooping = true
                r.play()
                ringtone = r
            } catch (e2: Exception) {
                // no sound available on this phone
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
            vibrator = v
        } catch (e: Exception) {
            // vibration is optional
        }
    }

    private fun onTimeout() {
        if (!isTest) {
            AlarmReceiver.showConfirm(this)
        }
        stopAlarm()
    }

    private fun releaseAll() {
        handler.removeCallbacks(timeout)
        try {
            player?.stop()
        } catch (e: Exception) {
            // already stopped
        }
        player?.release()
        player = null
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // already stopped
        }
        ringtone = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // already stopped
        }
        vibrator = null
        try {
            val lock = wakeLock
            if (lock != null && lock.isHeld) lock.release()
        } catch (e: Exception) {
            // already released
        }
        wakeLock = null
        ringing = false
    }

    private fun stopAlarm() {
        releaseAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIF_ID)
        stopSelf()
    }

    companion object {
        const val NOTIF_ID = 1
        private const val ALARM_TIMEOUT_MS = 5 * 60 * 1000L

        @Volatile
        var ringing = false

        fun start(context: Context, test: Boolean) {
            val intent = Intent(context, AlarmService::class.java).putExtra("test", test)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
