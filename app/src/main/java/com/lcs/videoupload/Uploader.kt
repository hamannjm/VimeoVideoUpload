package com.lcs.videoupload

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/* Upload flow:
    - Get DocumentFile from Uri
    - POST to Vimeo to begin upload process
    - Read in file as byte array from Uri
    - PATCH to Vimeo
 */

class Uploader(
    private val context: Context,
    private val fileUri: Uri
){

    private var statusCallback: UploadStatusCallback? = null

    private var currentUploadOffset = 0L
    private lateinit var uploadInformation: UploadResponse
    private val uploadLocation: String
        get() {
            return uploadInformation.upload.uploadLink
        }

    interface UploadStatusCallback {
        fun onUploadStarted()
        fun onUploadSuccess()
        fun onUploadFailed(reason: String?)
    }

    private val fileDocument by lazy {
        DocumentFile.fromSingleUri(context, fileUri) ?: throw IllegalStateException("File must exist!")
    }

    private val length by lazy {
        fileDocument.length()
    }

    fun registerStatusCallback(callback: UploadStatusCallback) {
        statusCallback = callback
    }

    fun uploadVideo() {
        Timber.d("Uploading file of size: $length")
        statusCallback?.onUploadStarted()
        Networking.videoUploadService.createUploadLocation(
            UploadRequest.createRequest(length)
        ).enqueue(
            object : Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    if (response.isSuccessful) {
                        uploadInformation = response.body()!!
                        uploadNextChunk()
                    } else {
                        statusCallback?.onUploadFailed("Something went wrong with creating upload location!")
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Timber.e(t)
                    statusCallback?.onUploadFailed(t.message)
                }
            }
        )
    }

    fun retryCurrentChunk() {
        uploadNextChunk()
    }

    private fun uploadNextChunk() {
        val videoChunk = fileUri.readBytes(context, currentUploadOffset, CHUNK_SIZE, length)
        Networking.videoUploadService.uploadVideoChunk(
            uploadOffset = currentUploadOffset,
            uploadUri = uploadLocation,
            videoChunk = videoChunk.toRequestBody()
        ).enqueue(
            object : Callback<Unit> {
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    if (response.isSuccessful) {
                        handleVideoUploadResponse(response)
                    } else {
                        statusCallback?.onUploadFailed("Something went wrong with uploading video chunk!")
                    }
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    Timber.e(t)
                    statusCallback?.onUploadFailed(t.message)
                }
            }
        )
    }

    private fun handleVideoUploadResponse(
        response: Response<Unit>
    ) {
        currentUploadOffset = response.headers()["Upload-Offset"]!!.toLong()
        if (currentUploadOffset < length) {
            uploadNextChunk()
        } else {
            Timber.d("Video has been uploaded successfully!")
            statusCallback?.onUploadSuccess()
        }
    }


    companion object {
        private const val KILOBYTE = 1024
        private const val MEGABYTE = KILOBYTE * 1000
        private const val CHUNK_SIZE = 128 * MEGABYTE
    }
}