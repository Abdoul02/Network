package com.example.networkapps.models

import com.google.gson.annotations.SerializedName

data class DeviceInfo(

    @SerializedName("imei")
    val imei: String,
    @SerializedName("wifiInformation")
    var wifiInformation: List<WifiInformationDto>
)