package com.example.networkapps.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.networkapps.BuildConfig
import com.example.networkapps.R
import com.example.networkapps.models.DeviceInfo
import com.example.networkapps.network.ApiUtils
import com.example.networkapps.permissions.PermissionUtils
import com.example.networkapps.service.WifiBroadCastReceiver
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var dialog: Dialog
    private val PermissionsRequestCode = 123
    private lateinit var permissionUtils: PermissionUtils
    private var imei = "Not available"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val permissionList = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        permissionUtils = PermissionUtils(this, permissionList, PermissionsRequestCode)
        permissionUtils.checkPermissions()

        fab.setOnClickListener { view ->
            showNetworkInfo()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PermissionsRequestCode -> {
                val isPermissionsGranted = permissionUtils
                    .processPermissionsResult(requestCode, permissions, grantResults)

                if (isPermissionsGranted) {
                    checkFirstRun()
                } else {

                }
                return
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun showNetworkInfo() {

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiManager?.let { it ->

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

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val telephonyManager =
                    applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                telephonyManager?.let { telephoneManager ->
                    imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        telephoneManager.imei
                    } else {
                        telephoneManager.deviceId
                    }
                }
            }


            val gson = GsonBuilder().create()
            sendLog(imei, gson.toJson(infoList))
            setDialog(true, getString(R.string.loading))
        }
    }

    private fun sendLog(imei: String, wifiInformation: String) {

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDateTime = sdf.format(Date())
        ApiUtils.getAPIService().logWifiData(imei, wifiInformation,currentDateTime).enqueue(object :
            Callback<DeviceInfo> {

            override fun onFailure(call: Call<DeviceInfo>, t: Throwable) {
                if (this@MainActivity.dialog.isShowing) {
                    dialog.dismiss()
                }
                Log.e(TAG, "Unable to submit post to API. ${t.message}")
            }

            override fun onResponse(call: Call<DeviceInfo>, response: Response<DeviceInfo>) {

                if (this@MainActivity.dialog.isShowing) {
                    dialog.dismiss()
                }
                showResponse(response.body())
            }
        })
    }

    private fun setDialog(show: Boolean, display_message: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this@MainActivity)
        val vl = inflater.inflate(R.layout.progress, null)
        val message = vl.findViewById<View>(R.id.loadingMsg) as TextView
        message.text = display_message
        builder.setView(vl)
        dialog = builder.create()
        if (show) {
            dialog.show()
        } else {
            dialog.cancel()
        }
    }

    private fun showResponse(deviceInfo: DeviceInfo?) {

        val response = StringBuilder()
        response.append("IMEI: ${deviceInfo?.imei} \n")
        for (wifiInfo in deviceInfo!!.wifiInformation) {
            response.append("BSSID: ${wifiInfo.bssid}, SSID: ${wifiInfo.ssid}, Level: ${wifiInfo.level} \n")
        }
        tv_post_response.text = response
    }

    private fun checkFirstRun() {

        val currentVersionCode = BuildConfig.VERSION_CODE
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST)
        if (currentVersionCode == savedVersionCode)
        {
            return
        }
        else if (savedVersionCode == DOESNT_EXIST)
        {
            WifiBroadCastReceiver.setupAlarm(this, true)
        }
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).apply()
    }

    companion object {
        val TAG = "NetworkTest"//this::class.java.simpleName
        const val PREFS_NAME = "MyPrefsFile"
        const val PREF_VERSION_CODE_KEY = "version_code"
        const val DOESNT_EXIST = -1
    }
}
