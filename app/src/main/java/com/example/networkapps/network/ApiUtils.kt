package com.example.networkapps.network

object ApiUtils {

    private const val BASE_URL = "http://nexgencs.co.za/"

    fun getAPIService():IAPIService{

        return RetrofitClient.getClient(BASE_URL)!!.create(IAPIService::class.java)
    }
}