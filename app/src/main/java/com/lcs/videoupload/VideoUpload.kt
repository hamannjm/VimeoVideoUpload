package com.lcs.videoupload

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface VideoUpload {

    @POST("/me/videos")
    fun createUploadLocation(
        @Body upload: Upload
    ): Call<UploadResponse>

    @PATCH
    fun uploadVideoChunk(
        @Header("Tus-Resumable") tusResumableVer: String = "1.0.0",
        @Header("Upload-Offset") uploadOffset: Long,
        @Header("Content-Type") contentType: String = "application/offset+octet-stream",
        @Url uploadUri: String,
        @Body videoChunk: RequestBody
    ): Call<Unit>
}