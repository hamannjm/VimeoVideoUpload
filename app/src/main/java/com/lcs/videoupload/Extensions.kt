package com.lcs.videoupload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File

fun File.readBytes(start: Long, maxBytesToRead: Int): ByteArray = inputStream().use { input ->
    val remaining = if (start + length() > maxBytesToRead.toLong()) {
        (length() - start).toInt()
    } else {
        maxBytesToRead
    }
    val result = ByteArray(remaining)
    input.skip(start)
    input.read(result, 0, remaining)
    return@use result
}

fun ContentResolver.inputStream(uri: Uri) = openInputStream(uri) ?: throw IllegalStateException("Unable to read in file!")

fun Uri.readBytes(
    context: Context,
    start: Long,
    maxBytesToRead: Int,
    fileLength: Long
): ByteArray = context.contentResolver.inputStream(this).use { input ->
    val remaining = if (start + fileLength > maxBytesToRead.toLong()) {
        (fileLength - start).toInt()
    } else {
        maxBytesToRead
    }
    val result = ByteArray(remaining)
    input.skip(start)
    input.read(result, 0, remaining)
    return@use result
}