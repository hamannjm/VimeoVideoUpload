package com.lcs.videoupload

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

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

    fun readInBytesTest(): ByteArray {
        return fileUri.readBytes(context, 0L, CHUNK_SIZE, length)
    }

    fun uploadVideo() {
        Timber.d("Uploading file of size: $length")
        statusCallback?.onUploadStarted()
        Networking.videoUploadService.createUploadLocation(
            Upload(size = length)
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
        val requestBodyVideoChunk = videoChunk.toRequestBody()
        Networking.videoUploadService.uploadVideoChunk(
            uploadOffset = currentUploadOffset,
            uploadUri = uploadLocation,
            videoChunk = requestBodyVideoChunk
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
        private const val CHUNK_SIZE = 1 * MEGABYTE
    }
}