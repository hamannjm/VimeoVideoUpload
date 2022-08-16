package com.lcs.videoupload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.lcs.videoupload.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var uploader: Uploader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        intent.data?.let { callbackUri ->
            handleAuth(callbackUri)
        } ?: displayUserInfo()

        binding.chooser.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECTION)
        }

        binding.login.setOnClickListener {
            VimeoAuth.clearAccessToken()
            Intent(Intent.ACTION_VIEW, Uri.parse(Networking.OATH_URL)).run {
                startActivity(this)
            }
        }
        binding.defaultUser.setOnClickListener {
            VimeoAuth.defaultUser {
                displayUserInfo()
            }
        }
    }

    private fun displayUserInfo() {
        VimeoAuth.currentUser?.let {
            binding.upload.isEnabled = true
            binding.currentUser.text = "${it.user.name}\r\n${it.token}"
        } ?: run {
            VimeoAuth.obtainUserInformation {
                displayUserInfo()
            }
        }
    }

    private fun handleAuth(callbackUri: Uri) {
        val accessCode = callbackUri.getQueryParameters("code").single()
        VimeoAuth.obtainAccessToken(accessCode) {
            displayUserInfo()
        }
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