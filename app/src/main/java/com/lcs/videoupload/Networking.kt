package com.lcs.videoupload

import android.content.Context
import android.net.Uri
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.squareup.moshi.Moshi
import com.vimeo.networking2.*
import com.vimeo.networking2.config.VimeoApiConfiguration
import com.vimeo.networking2.logging.LogDelegate
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Networking {
    private const val BASE_URL = "https://api.vimeo.com/"
    private const val CALLBACK_URL = "my.scheme://com.lcs.inspectionvideos/auth"
    private const val CLIENT_ID = "74fff6d14ce9f4c59c09cb25832d384ce47d685c"
    private const val CLIENT_SECRET = "Io67zF9XuL44jjOKi9LDkv65E50Rgwpq0WXjG03vsc5i0cf4tbsnSFh029f8QL2tN9GLzRcdJHF3dnt8gPvtzPy+TDFaU1uikUTSEmD4dPT4uB30xaci0I9feEtTXU1b"

    private val moshi = Moshi.Builder()
        .build()

    private lateinit var vimeoConfig: VimeoApiConfiguration
    private val vimeoAuth by lazy { com.vimeo.networking2.Authenticator(vimeoConfig) }

    private lateinit var httpClient: OkHttpClient
    private lateinit var retrofit: Retrofit

    fun init(context: Context) {
        httpClient = OkHttpClient.Builder()
            .addInterceptor(VimeoInterceptor())
            .addInterceptor(DebugInterceptor())
            .addInterceptor(chucker(context))
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(moshi)
            )
            .build()
        vimeoConfig = VimeoApiConfiguration.Builder(
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET,
            scopes = listOf(
                ScopeType.PUBLIC,
                ScopeType.EDIT,
                ScopeType.UPLOAD
            )
        )
            .withApplicationInterceptors(
                listOf(chucker(context))
            )
            .withCodeGrantRedirectUrl(CALLBACK_URL)
            .withLogLevel(LogDelegate.Level.DEBUG)
            .build()
    }

    private fun chucker(context: Context) = ChuckerInterceptor
        .Builder(context)
        .build()

    private val vimeoApi: VimeoApiClient by lazy { VimeoApiClient(vimeoConfig, vimeoAuth) }

    val videoUploadService: VideoUpload by lazy { retrofit.create(VideoUpload::class.java) }

    fun createLoginUriWithRedirect(responseCode: String): Uri = vimeoAuth
        .createCodeGrantAuthorizationUri(responseCode)
        .run { Uri.parse(this) }

    fun authenticateWithAccessCodeRedirectUri(
        redirectUri: Uri,
        response: VimeoCallback<VimeoAccount>
    ) {
        vimeoAuth.authenticateWithCodeGrant(
            redirectUri.toString().replace("my.scheme", "https"),
            response
        )
    }

    suspend fun authenticate(
        redirectUri: Uri
    ) = toSuspendFun(vimeoAuth::authenticateWithCodeGrant)(
        redirectUri.toString().replace("my.scheme", "https")
    )

    fun getVideos(
        response: VimeoCallback<VideoList>
    ) {
        val videoListUri = vimeoAuth.currentAccount?.user?.metadata?.connections?.videos?.uri
            ?: return run {
                response.onError(
                    VimeoResponse.Error.Unknown("Invalid user!", -1)
                )
            }
        vimeoApi.fetchVideoList(
            uri = videoListUri,
            fieldFilter = null,
            queryParams = null,
            cacheControl = null,
            callback = response
        )
    }

    fun getAccessToken() = vimeoAuth.currentAccount?.accessToken ?: throw Throwable("Not authorized!")

    fun isAuthenticated() = vimeoAuth.currentAccount?.user != null
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
                .addHeaderIfNotPresent("Authorization", "bearer " + Networking.getAccessToken())
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