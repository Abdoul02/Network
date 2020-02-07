package com.example.networkapps.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class WifiBroadCastReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        val serviceIntent:Intent
        if (ACTION_START_NOTIFICATION_SERVICE == action)
        {
            Log.i("WifiBroadCastReceiver", "onReceive from alarm, starting notification service")
            serviceIntent = Intent(context, WifiLogService::class.java)
            serviceIntent.action = ACTION_START
            ContextCompat.startForegroundService(context!!, serviceIntent)
        }
        else if (ACTION_BOOT_COMPLETED == action)
        {
            Log.i("WifiBroadCastReceiver", "Device Boot Completed")
            setupAlarm(context!!, false)
        }
    }


    companion object {
        private const val ACTION_START_NOTIFICATION_SERVICE = "ACTION_START_NOTIFICATION_SERVICE"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        private const val ACTION_START = "ACTION_START"
        const val HOUR_INTERVAL = 3600000 //* 2 // 2Hours
        private const val TWO_MINUTE = 60000 * 2

        private fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = getStartPendingIntent(context)
            alarmManager.cancel(alarmIntent)
        }

        fun setupAlarm(context: Context, force: Boolean) {
            cancelAlarm(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = getStartPendingIntent(context)
            var currentTime = System.currentTimeMillis()
            if (!force) {
                currentTime += HOUR_INTERVAL.toLong()
            }
            val sdkInt = Build.VERSION.SDK_INT
            when {
                sdkInt < Build.VERSION_CODES.KITKAT -> alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    alarmIntent
                )
                (Build.VERSION_CODES.KITKAT <= sdkInt) && sdkInt < Build.VERSION_CODES.M -> alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    alarmIntent
                )
                sdkInt >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        currentTime,
                        alarmIntent
                    )
                }
            }
        }

        private fun getStartPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WifiBroadCastReceiver::class.java)
            intent.action = ACTION_START_NOTIFICATION_SERVICE
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
