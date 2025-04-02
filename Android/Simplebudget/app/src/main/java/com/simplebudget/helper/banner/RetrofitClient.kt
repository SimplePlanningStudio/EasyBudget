package com.simplebudget.helper.banner

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.simplebudget.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException


object RetrofitClient {
    private const val BASE_URL = "BASE_URL"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)    // Connection timeout
        .readTimeout(30, TimeUnit.SECONDS)       // Read timeout
        .writeTimeout(30, TimeUnit.SECONDS)      // Write timeout
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request) // Proceed with the request
            } catch (e: SocketTimeoutException) {
                logAndHandleError("socket_timeout", e)
                throw e
            } catch (e: UnknownHostException) {
                logAndHandleError("unknown_host", e)
                throw e
            } catch (e: ConnectException) {
                logAndHandleError("connect_exception", e)
                throw e
            } catch (e: SSLException) {
                logAndHandleError("ssl_exception", e)
                throw e
            } catch (e: IOException) {
                if (e.message?.contains("timeout", true) == true) {
                    logAndHandleError("network_timeout", e)
                } else {
                    logAndHandleError("io_exception", e)
                }
                throw e
            } catch (e: Exception) {
                logAndHandleError("unknown_exception", e)
                throw e
            }
        }
        .build()

    val instance: BannerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BannerApiService::class.java)
    }

    // Helper method to log and handle errors safely
    private fun logAndHandleError(eventName: String, exception: Exception) {
        try {
            val bundle = Bundle().apply {
                putString("error_message", exception.message)
                putString("error_type", eventName)
            }

            Firebase.analytics.logEvent(eventName, bundle)

            if (BuildConfig.DEBUG) {
                exception.printStackTrace()
            }
            // Handle specific errors safely (optional fallback logic here)
            when (eventName) {
                "socket_timeout", "network_timeout" -> {
                    // Handle timeout
                }

                "unknown_host", "connect_exception" -> {
                    // Handle no internet connection
                }

                "ssl_exception" -> {
                    // Handle SSL exceptions
                }

                "io_exception" -> {
                    // Handle IO errors
                }

                "unknown_exception" -> {
                    // Handle unexpected errors
                }
            }
        } catch (_: Exception) {
        }
    }
}

