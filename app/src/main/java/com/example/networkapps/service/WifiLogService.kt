package com.example.networkapps.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.example.networkapps.R
import com.example.networkapps.activities.MainActivity
import com.example.networkapps.models.DeviceInfo
import com.example.networkapps.network.ApiUtils
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class WifiLogService : IntentService("WifiLogService") {

    private lateinit var wakeLock: PowerManager.WakeLock
    private var imei = "Not available"

    init {
        setIntentRedelivery(true)
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WifiApp:Wakelock"
        )
        wakeLock.acquire()
        Log.d(TAG, "Wakelock acquired")
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Wifi IntentService")
            .setContentText("Running...")
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)
    }

    override fun onHandleIntent(intent: Intent?) {

        Log.d(TAG, "IntentAction: ${intent!!.action}")
        val action = intent!!.getAction()
        if (ACTION_START == action) {
            uploadWifiInfo(intent)
        }
    }

    @SuppressLint("HardwareIds")
    private fun uploadWifiInfo(intent: Intent) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val phoneStatePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        val locationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d(
            TAG,
            "Permission ${phoneStatePermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED}"
        )
        if (phoneStatePermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED) {

            wifiManager?.let {
                val wifiList = it.scanResults
                val infoList = arrayListOf<HashMap<String, String>>()
                for (scanResult in wifiList) {
                    val map = HashMap<String, String>()
                    val level = WifiManager.calculateSignalLevel(scanResult.level, 5)
                    map["bssid"] = scanResult.BSSID
                    map["ssid"] = scanResult.SSID
                    map["level"] = level.toString()
                    infoList.add(map)
                }

                val telephonyManager =
                    applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                telephonyManager?.let { telephoneManager ->
                    imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        telephoneManager.imei
                    } else {
                        telephoneManager.deviceId
                    }
                }
                val gson = GsonBuilder().create()
                sendLog(imei, gson.toJson(infoList))
            }
        }

        /* reset the alarm */
        WifiBroadCastReceiver.setupAlarm(this, false)
        Log.d(TAG, "Alarm Reset ")

    }


    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
        Log.d(TAG, "Wakelock released")
    }

    private fun sendLog(imei: String, wifiInformation: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDateTime = sdf.format(Date())
        ApiUtils.getAPIService().logWifiData(imei, wifiInformation, currentDateTime)
            .enqueue(object :
                Callback<DeviceInfo> {

                override fun onFailure(call: Call<DeviceInfo>, t: Throwable) {
                    Log.e(TAG, "Unable to submit post to API. ${t.message}")
                }

                override fun onResponse(call: Call<DeviceInfo>, response: Response<DeviceInfo>) {
                    Log.d(TAG, "Response. ${response.body().toString()}")
                    showNotification(
                        this@WifiLogService,
                        response.body(),
                        System.currentTimeMillis().toInt()
                    )
                }
            })
    }

    fun showNotification(context: Context, deviceInfo: DeviceInfo?, requestCode: Int) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)
        val pendingIntent =
            stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
        val notification = builder.setContentTitle("Wifi info")
            .setContentText("${deviceInfo?.wifiInformation!!.size} available wifi information stored")
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .build()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestCode, notification)
    }


    companion object {
        val TAG = "WifiLogService"
        private const val SERVICE_CHANNEL_ID = "foreground_service"
        private const val CHANNEL_ID = "Rate_alert";
        private val NOTIFICATION_ID = 1
        private val ACTION_START = "ACTION_START"
        private val ACTION_DELETE = "ACTION_DELETE"
    }
}