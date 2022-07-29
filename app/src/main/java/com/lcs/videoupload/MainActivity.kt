package com.lcs.videoupload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.lcs.videoupload.databinding.ActivityMainBinding
import com.vimeo.networking2.Authenticator
import com.vimeo.networking2.ScopeType
import com.vimeo.networking2.VimeoApiClient
import com.vimeo.networking2.config.VimeoApiConfiguration
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var uploader: Uploader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
//        verifyPermissions()
        binding.chooser.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECTION)
        }
    }

    private fun verifyPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            FILE_PERMISSION
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