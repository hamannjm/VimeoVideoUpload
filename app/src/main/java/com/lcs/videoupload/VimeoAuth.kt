package com.lcs.videoupload

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit

@Suppress("StaticFieldLeak")
object VimeoAuth {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    private const val AUTH_TOKEN_KEY = "vimeoAuthToken"

    fun init(appContext: Context) {
        context = appContext
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun isAuthenticated() = getAccessToken() == null

    fun getAccessToken(): String? {
        return preferences.getString(AUTH_TOKEN_KEY, null)
    }

    fun setAccessToken(token: String) {
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