package com.lcs.videoupload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Authentication(
    @Json(name = "grant_type")
    val grantType: String = "authorization_code",
    val code: String,
    @Json(name = "redirect_uri")
    val redirectUri: String = VimeoAuth.CALLBACK_URL
)

@JsonClass(generateAdapter = true)
data class AuthenticationResponse(
    @Json(name = "access_token")
    val token: String,
    @Json(name = "token_type")
    val tokenType: String?,
    val scope: String,
    val app: VimeoApp,
    val user: VimeoUser
)

@JsonClass(generateAdapter = true)
data class VimeoApp(
    val name: String,
    val uri: String?
)

@JsonClass(generateAdapter = true)
data class VimeoUser(
    val name: String,
    val uri: String?,
    @Json(name = "upload_quota")
    val uploadQuota: UploadQuota?
)

@JsonClass(generateAdapter = true)
data class UploadQuota(
    val space: UploadSpace,
    val periodic: PeriodicSpace,
    val lifetime: TotalSpace
)

@JsonClass(generateAdapter = true)
data class UploadSpace(
    val free: Long,
    val max: Long,
    val used: Long,
    val showing: String
)

@JsonClass(generateAdapter = true)
data class PeriodicSpace(
    val period: String,
    val free: Long,
    val max: Long,
    val used: Long
)

@JsonClass(generateAdapter = true)
data class TotalSpace(
    val free: Long,
    val max: Long,
    val used: Long
)

/*
{
"upload_quota": {
            "space": {
                "free": 524288000,
                "max": 524288000,
                "used": 0,
                "showing": "periodic",
                "unit": "video_size"
            },
            "periodic": {
                "period": "week",
                "unit": "video_size",
                "free": 524288000,
                "max": 524288000,
                "used": 0,
                "reset_date": "2022-07-29T00:00:00-04:00"
            },
            "lifetime": {
                "unit": "video_size",
                "free": 5368709120,
                "max": 5368709120,
                "used": 0
            }
        }
 */