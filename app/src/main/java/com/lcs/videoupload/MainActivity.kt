package com.lcs.videoupload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        verifyPermissions()
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
                        Uploader(this, fileUri).uploadVideo()
                    }
                }
            }
        }

    }

    companion object {
        const val FILE_SELECTION = 7878
        const val FILE_PERMISSION = 5131
    }
}