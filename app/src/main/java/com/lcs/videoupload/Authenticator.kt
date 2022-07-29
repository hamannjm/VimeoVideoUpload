package com.lcs.videoupload

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface Authenticator {

    @POST("/oauth/access_token")
    fun obtainAccessToken(
        @Header("Authorization") clientIdentifier: String,
        @Body authentication: Authentication
    ): Call<AuthenticationResponse>
}