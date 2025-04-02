package com.simplebudget.helper.banner

import com.google.gson.annotations.SerializedName

data class AppBanner(
    @SerializedName("app_name") val appName: String?,
    @SerializedName("package_name") val packageName: String?,
    @SerializedName("show_banner") val showBanner: Boolean = false,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("redirect_url") val redirectUrl: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
)

data class BannerResponse(
    @SerializedName("apps") val apps: List<AppBanner>
)