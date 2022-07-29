package com.lcs.videoupload

import android.net.Uri
import com.squareup.moshi.Moshi
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import timber.log.Timber
import java.util.concurrent.TimeUnit

object Networking {
    private const val BASE_URL = "https://api.vimeo.com/"
    const val ACCESS_TOKEN = "5d19ea99ac8c99542da11cfe32d7fa82"
    const val SECONDARY_TOKEN = "47677c54e4169844b5a2d3c32ee619ea"

    val oathUri = Uri.encode("https://api.vimeo.com/oauth/authorize?response_type=code&client_id=74fff6d14ce9f4c59c09cb25832d384ce47d685c&redirect_uri=https://com.lcs.inspectionvideos/auth&state=12345&scope=upload edit")

    private val moshi = Moshi.Builder()
        .build()

    val httpClient = OkHttpClient.Builder()
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .connectTimeout(300, TimeUnit.SECONDS)
        .addInterceptor(VimeoInterceptor())
        .addInterceptor(DebugInterceptor())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(
            MoshiConverterFactory.create(moshi)
        )
        .build()

    val videoUploadService = retrofit.create(VideoUpload::class.java)
    val authenticationService = retrofit.create(Authenticator::class.java)
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
                .addHeaderIfNotPresent("Authorization", "bearer " + Networking.ACCESS_TOKEN)
                .addHeaderIfNotPresent("Content-Type", "application/json")
                .add("Accept", "application/vnd.vimeo.*+json;version=3.4")
                .build()
            req.newBuilder().headers(updatedHeaders).build()
        }
    )

    private fun Headers.Builder.addHeaderIfNotPresent(key: String, value: String) = get(key)?.let {
        this
    } ?: add(key, value)
}