package com.lcs.videoupload

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64
import androidx.core.content.edit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

@Suppress("StaticFieldLeak")
object VimeoAuth {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    private const val AUTH_TOKEN_KEY = "vimeoAuthToken"

    const val CALLBACK_URL = "my.scheme://com.lcs.inspectionvideos/auth"
    const val CLIENT_ID = "74fff6d14ce9f4c59c09cb25832d384ce47d685c"
    const val CLIENT_SECRET = "Io67zF9XuL44jjOKi9LDkv65E50Rgwpq0WXjG03vsc5i0cf4tbsnSFh029f8QL2tN9GLzRcdJHF3dnt8gPvtzPy+TDFaU1uikUTSEmD4dPT4uB30xaci0I9feEtTXU1b"

    fun init(appContext: Context) {
        context = appContext
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun obtainAccessToken(accessCode: String, onSuccess: () -> Unit) {
        Networking.authenticationService.obtainAccessToken(
            "basic " + Base64.encodeToString(
                "$CLIENT_ID:$CLIENT_SECRET".encodeToByteArray(),
                Base64.NO_WRAP or Base64.NO_PADDING
            ),
            Authentication(
                code = accessCode
            )
        ).enqueue(
            object : Callback<AuthenticationResponse> {
                override fun onResponse(
                    call: Call<AuthenticationResponse>,
                    response: Response<AuthenticationResponse>
                ) {
                    if (!response.isSuccessful) {
                        Timber.e("Auth response was unsuccessful")
                        return
                    }

                    val token = response.body()!!.token
                    setAccessToken(token)
                    onSuccess()
                }

                override fun onFailure(call: Call<AuthenticationResponse>, t: Throwable) {
                    Timber.e(t)
                }
            }
        )
    }

    fun isAuthenticated() = getAccessToken() != null

    fun getAccessToken() = preferences.getString(AUTH_TOKEN_KEY, null)

    private fun setAccessToken(token: String) {
        preferences.edit(commit = true) {
            putString(AUTH_TOKEN_KEY, token)
        }
    }

    fun clearAccessToken() {
        preferences.edit(commit = true) {
            putString(AUTH_TOKEN_KEY, null)
        }
    }

}