package com.lcs.videoupload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.vimeo.networking2.VimeoCallback
import com.vimeo.networking2.VimeoResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun ContentResolver.inputStream(uri: Uri) = openInputStream(uri) ?: throw IllegalStateException("Unable to read in file!")

fun Uri.readBytes(
    context: Context,
    start: Long,
    maxBytesToRead: Int,
    fileLength: Long
): ByteArray = context.contentResolver.inputStream(this).use { input ->
    // start -> some number of bytes.
    // if the fileLength - start is less than the max then we just want to read in the remainder
    // if the fileLength - start is greater than the max then read in the max
    val remainingBytesInFile = fileLength - start
    val numberOfBytesToRead = if (remainingBytesInFile < maxBytesToRead.toLong()) {
        remainingBytesInFile.toInt()
    } else {
        maxBytesToRead
    }
    val result = ByteArray(numberOfBytesToRead)
    input.skip(start)
    input.read(result, 0, numberOfBytesToRead)
    return@use result
}


suspend fun <T> awaitCallback(block: (VimeoCallback<T>) -> Unit): T =
    suspendCancellableCoroutine { cancellableContinuation ->
        block(object : VimeoCallback<T> {
            override fun onError(error: VimeoResponse.Error) {
                cancellableContinuation.resumeWithException(Throwable(error.message))
            }

            override fun onSuccess(response: VimeoResponse.Success<T>) {
                cancellableContinuation.resume(response.data)
            }
        })
    }

suspend fun <A, T> toSuspendFun(fn: (A, VimeoCallback<T>) -> Unit): suspend (A) -> T = { a: A ->
    awaitCallback { fn(a, it) }
}