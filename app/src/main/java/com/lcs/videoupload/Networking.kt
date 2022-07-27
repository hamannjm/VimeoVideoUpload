package com.lcs.videoupload

import com.squareup.moshi.Moshi
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

object Networking {
    private const val BASE_URL = "https://api.vimeo.com/"
    const val ACCESS_TOKEN = "5d19ea99ac8c99542da11cfe32d7fa82"

    private val moshi = Moshi.Builder()
        .build()

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(DebugInterceptor())
        .addInterceptor(VimeoInterceptor())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(
            MoshiConverterFactory.create(moshi)
        )
        .build()

    val videoUploadService = retrofit.create(VideoUpload::class.java)
}

class DebugInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request()).apply {
            peekBody(Long.MAX_VALUE).apply {
                Timber.d(string())
            }
        }
    }
}

class VimeoInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().let { req ->
            val updatedHeaders = req.headers.newBuilder()
                .add("Authorization", "bearer " + Networking.ACCESS_TOKEN)
                .addContentTypeIfNotPresent()
                .add("Accept", "application/vnd.vimeo.*+json;version=3.4")
                .build()
            req.newBuilder().headers(updatedHeaders).build()
        }
    )

    private fun Headers.Builder.addContentTypeIfNotPresent() = get("Content-Type")?.let {
        this
    } ?: add("Content-Type", "application/json")
}