package com.lcs.videoupload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.lcs.videoupload.databinding.ActivityMainBinding
import com.vimeo.networking2.VideoList
import com.vimeo.networking2.VimeoAccount
import com.vimeo.networking2.VimeoCallback
import com.vimeo.networking2.VimeoResponse
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var uploader: Uploader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        authenticate(intent.data)

        binding.chooser.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECTION)
        }
    }

    private fun authenticate(callbackUri: Uri?) {
        if (Networking.isAuthenticated()) {
            binding.upload.isEnabled = true
            fetchVideos()
            return
        }
        if (callbackUri == null) {
            Intent(
                Intent.ACTION_VIEW,
                Networking.createLoginUriWithRedirect("12345")
            ).run {
                startActivity(this)
            }
        } else {
            Networking.authenticateWithAccessCodeRedirectUri(
                callbackUri,
                response = object : VimeoCallback<VimeoAccount> {
                    override fun onError(error: VimeoResponse.Error) {
                        Timber.e(error.message)
                    }

                    override fun onSuccess(response: VimeoResponse.Success<VimeoAccount>) {
                        binding.upload.isEnabled = true
                        makeToast("Logged in successfully!")
                        fetchVideos()
                    }
                }
            )
        }
    }

    private fun fetchVideos() {
        Networking.getVideos(
            object : VimeoCallback<VideoList> {
                override fun onError(error: VimeoResponse.Error) {
                    Timber.e(error.message)
                }

                override fun onSuccess(response: VimeoResponse.Success<VideoList>) {
                    val videos = response.data.data ?: listOf()
                    Timber.d(videos.map { it.toString() + "\r\n" }.toString())
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_SELECTION -> {
                data?.data?.let { fileUri ->
                    binding.chosenFile.text = fileUri.toString()
                    binding.upload.setOnClickListener {
                        binding.retry.isVisible = true
                        uploader = Uploader(this, fileUri).apply {
                            registerStatusCallback(
                                object : Uploader.UploadStatusCallback {
                                    override fun onUploadStarted() {
                                        makeToast("Beginning upload!")
                                    }

                                    override fun onUploadSuccess() {
                                        makeToast("Uploaded successfully!")
                                    }

                                    override fun onUploadFailed(reason: String?) {
                                        makeToast("Upload failed: ${reason ?: "unknown error"}")
                                    }
                                }
                            )
                            uploadVideo()
                        }
                    }
                    binding.retry.setOnClickListener {
                        uploader.retryCurrentChunk()
                    }
                }
            }
        }

    }

    fun makeToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        const val FILE_SELECTION = 7878
        const val FILE_PERMISSION = 5131
    }
}