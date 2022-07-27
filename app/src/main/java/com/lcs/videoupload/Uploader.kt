package com.lcs.videoupload

import android.content.ContentProvider
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class Uploader(
    private val context: Context,
    private val fileUri: Uri
){

    private val fileDocument by lazy {
        DocumentFile.fromSingleUri(context, fileUri) ?: throw IllegalStateException("File must exist!")
    }

    private val length by lazy {
        fileDocument.length()
    }

    fun uploadVideo() {
        Timber.d("Uploading file of size: $length")
        Networking.videoUploadService.createUploadLocation(
            Upload(size = length)
        ).enqueue(
            object : Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    if (response.isSuccessful) {
                        onCreateUploadLocationSuccess(response.body()!!)
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Timber.e(t)
                }
            }
        )
    }

    private fun onCreateUploadLocationSuccess(
        response: UploadResponse
    ) {
        uploadVideoChunk(0L, response.uploadLink!!)
    }

    private fun uploadVideoChunk(
        offset: Long,
        destination: String
    ) {
        val videoChunk = fileUri.readBytes(context, offset, CHUNK_SIZE, length)
        Networking.videoUploadService.uploadVideoChunk(
            uploadOffset = offset,
            uploadUri = destination,
            videoChunk = videoChunk
        ).enqueue(
            object : Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    handleVideoUploadResponse(response, destination)
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Timber.e(t)
                }
            }
        )
    }

    private fun handleVideoUploadResponse(
        response: Response<UploadResponse>,
        destination: String
    ) {
        if (!response.isSuccessful) {
            //notify UI
            Timber.d("Something went wrong with the upload!")
            return
        }
        val newOffset = response.headers()["Upload-Offset"]!!.toLong()
        if (newOffset < length) {
            uploadVideoChunk(newOffset, destination)
        }
    }


    companion object {
        private const val KILOBYTE = 1024
        private const val MEGABYTE = KILOBYTE * 1000
        private const val CHUNK_SIZE = 128 * MEGABYTE
    }
}