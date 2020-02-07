package com.example.networkapps.network

import com.example.networkapps.models.DeviceInfo
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

interface IAPIService {

    @POST("/api/wifiInfo.php")
    @FormUrlEncoded
    fun logWifiData(
        @Field("imei") imei: String,
        @Field("wifiInformation") wifiInformation: String,
        @Field("dateTime") dateTime: String
    ): Call<DeviceInfo>
}