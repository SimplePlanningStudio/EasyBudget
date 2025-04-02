package com.simplebudget.helper.banner

import retrofit2.Call
import retrofit2.http.GET

interface BannerApiService {
    @GET("banner_data_simplebudget.json")
    suspend fun getBanner(): BannerResponse
}
