package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.PillboxEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON" || action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            Log.d("ReminderReceiver", "System booted. Rescheduling all active reminders...")
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val allEntries = db.pillboxDao().getAllPillboxEntriesDirect()
                    allEntries.forEach { entry ->
                        schedulePillReminder(
                            context = context,
                            entryId = entry.id,
                            medicineName = entry.medicineName,
                            dosage = entry.dosage,
                            timeStr = entry.preferredTime,
                            periodicityDays = entry.periodicityDays
                        )
                    }
                    Log.d("ReminderReceiver", "Rescheduled ${allEntries.size} reminders successfully on boot.")
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val entryId = intent.getIntExtra("entry_id", -1)
        val medicineName = intent.getStringExtra("medicine_name") ?: "Лекарство"
        val dosage = intent.getDoubleExtra("dosage", 1.0)
        val timeStr = intent.getStringExtra("time_str") ?: "12:00"
        val periodicityDays = intent.getIntExtra("periodicity_days", 1)

        Log.d("ReminderReceiver", "Alarm received for entry $entryId, medicine: $medicineName at $timeStr")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                // Get all medicines scheduled for this time across pillboxes
                val db = AppDatabase.getDatabase(context)
                val allEntries = db.pillboxDao().getAllPillboxEntriesDirect()
                
                // Filter matching entries
                val matchingEntries = allEntries.filter { it.preferredTime == timeStr }
                
                val medicinesToTake = if (matchingEntries.isNotEmpty()) {
                    matchingEntries
                } else {
                    listOf(
                        PillboxEntry(
                            id = entryId,
                            pillboxId = 0,
                            medicineName = medicineName,
                            dosage = dosage,
                            preferredTime = timeStr,
                            periodicityDays = periodicityDays
                        )
                    )
                }

                // Format: Name - dosage шт
                val formattedLines = medicinesToTake.map { entry ->
                    val dosageStr = if (entry.dosage % 1.0 == 0.0) entry.dosage.toInt().toString() else entry.dosage.toString()
                    "${entry.medicineName} - $dosageStr шт."
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "pillbox_reminders_channel"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Таблетница МедСкан",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Напоминания о приеме назначенных лекарств"
                        enableLights(true)
                        enableVibration(true)
                        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        setSound(defaultSoundUri, null)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                // Action when user clicks the notification (open the app)
                val openIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    putExtra("navigate_to_tab", "PILLBOX")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                
                // Use preferredTime's hash as requestCode to allow distinct notifications or updates for different times
                val requestCode = timeStr.hashCode()
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentTitle = "Время приема лекарств! ($timeStr)"
                val contentText = formattedLines.joinToString(", ")
                val bigTextMessage = "Пожалуйста, примите следующие лекарства:\n" + formattedLines.joinToString("\n") { "• $it" }
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.example.R.mipmap.ic_launcher)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(defaultSoundUri)
                    .setVibrate(longArrayOf(0, 300, 200, 300))
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextMessage))

                notificationManager.notify(requestCode, builder.build())

                // RESCHEDULE for the next occurrence!
                if (entryId != -1) {
                    schedulePillReminder(context, entryId, medicineName, dosage, timeStr, periodicityDays)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun schedulePillReminder(
            context: Context,
            entryId: Int,
            medicineName: String,
            dosage: Double,
            timeStr: String,
            periodicityDays: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Compute correct next day depending on custom periodicity
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, periodicityDays)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("entry_id", entryId)
                putExtra("medicine_name", medicineName)
                putExtra("dosage", dosage)
                putExtra("time_str", timeStr)
                putExtra("periodicity_days", periodicityDays)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                entryId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Extremely robust: setAlarmClock guarantees physical wake and bypass of sleeping/killing.
            try {
                var scheduled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val showPendingIntent = PendingIntent.getActivity(
                            context,
                            entryId,
                            showIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(
                            calendar.timeInMillis,
                            showPendingIntent
                        )
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                        scheduled = true
                        Log.d("ReminderReceiver", "Scheduled AlarmClock for entry $entryId to $timeStr in $periodicityDays day(s). At: ${calendar.time}")
                    }
                } else {
                    val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        entryId,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        showPendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    scheduled = true
                    Log.d("ReminderReceiver", "Scheduled AlarmClock for entry $entryId to $timeStr in $periodicityDays day(s). At: ${calendar.time}")
                }

                if (!scheduled) {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("ReminderReceiver", "Scheduled fallback setAndAllowWhileIdle for entry $entryId to $timeStr")
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to schedule exact alarm/AlarmClock, attempting fallback...", e)
                try {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("ReminderReceiver", "Scheduled standard fallback setAndAllowWhileIdle for entry $entryId")
                } catch (ex: Exception) {
                    Log.e("ReminderReceiver", "Double fallback failure, scheduling basic set", ex)
                    try {
                        alarmManager.set(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } catch (finalEx: Exception) {
                        Log.e("ReminderReceiver", "Critical: Failed to schedule any alarm", finalEx)
                    }
                }
            }
        }

        fun cancelPillReminder(context: Context, entryId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                entryId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("ReminderReceiver", "Cancelled alarm for entry $entryId")
        }
    }
}
