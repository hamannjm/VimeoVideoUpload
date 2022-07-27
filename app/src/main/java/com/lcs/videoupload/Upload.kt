package com.lcs.videoupload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Upload(
    val approach: String = "tus",
    val size: Long
)

@JsonClass(generateAdapter = true)
data class UploadResponse(
    val status: String,
    @Json(name = "upload_link")
    val uploadLink: String?,
    val form: String?,
    @Json(name = "complete_uri")
    val completeUri: String?,
    val approach: String?,
    val size: Long?,
    @Json(name = "redirect_uri")
    val redirectUri: String?,
    val link: String?
)

/*
    "upload": {
        "status": "in_progress",
        "upload_link": "https://us-files.tus.vimeo.com/files/vimeo-prod-src-tus-us/31d1067ae29ad05330c651e18c09dd58",
        "form": null,
        "complete_uri": null,
        "approach": "tus",
        "size": 1024,
        "redirect_url": null,
        "link": null
    }
 */