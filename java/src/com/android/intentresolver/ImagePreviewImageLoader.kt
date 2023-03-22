/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.intentresolver

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.android.intentresolver.contentpreview.ImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.function.Consumer

private const val TAG = "ImagePreviewImageLoader"

/**
 * Implements preview image loading for the content preview UI. Provides requests deduplication and
 * image caching.
 */
@VisibleForTesting
class ImagePreviewImageLoader @JvmOverloads constructor(
    private val context: Context,
    private val lifecycle: Lifecycle,
    cacheSize: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageLoader {

    private val thumbnailSize: Size =
        context.resources.getDimensionPixelSize(R.dimen.chooser_preview_image_max_dimen).let {
            Size(it, it)
        }

    private val lock = Any()
    @GuardedBy("lock")
    private val cache = LruCache<Uri, RequestRecord>(cacheSize)
    @GuardedBy("lock")
    private val runningRequests = HashMap<Uri, RequestRecord>()

    override suspend fun invoke(uri: Uri, caching: Boolean): Bitmap? = loadImageAsync(uri, caching)

    override fun loadImage(uri: Uri, callback: Consumer<Bitmap?>) {
        lifecycle.coroutineScope.launch {
            val image = loadImageAsync(uri, caching = true)
            if (isActive) {
                callback.accept(image)
            }
        }
    }

    override fun prePopulate(uris: List<Uri>) {
        uris.asSequence().take(cache.maxSize()).forEach { uri ->
            lifecycle.coroutineScope.launch {
                loadImageAsync(uri, caching = true)
            }
        }
    }

    private suspend fun loadImageAsync(uri: Uri, caching: Boolean): Bitmap? {
        return getRequestDeferred(uri, caching)
            .await()
    }

    private fun getRequestDeferred(uri: Uri, caching: Boolean): Deferred<Bitmap?> {
        var shouldLaunchImageLoading = false
        val request = synchronized(lock) {
            cache[uri]
                ?: runningRequests.getOrPut(uri) {
                    shouldLaunchImageLoading = true
                    RequestRecord(uri, CompletableDeferred(), caching)
                }.apply {
                    this.caching = this.caching || caching
                }
        }
        if (shouldLaunchImageLoading) {
            request.loadBitmapAsync()
        }
        return request.deferred
    }

    private fun RequestRecord.loadBitmapAsync() {
        lifecycle.coroutineScope.launch(dispatcher) {
            loadBitmap()
        }.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                cancel()
            }
        }
    }

    private fun RequestRecord.loadBitmap() {
        val bitmap = try {
            context.contentResolver.loadThumbnail(uri,  thumbnailSize, null)
        } catch (t: Throwable) {
            Log.d(TAG, "failed to load $uri preview", t)
            null
        }
        complete(bitmap)
    }

    private fun RequestRecord.cancel() {
        synchronized(lock) {
            runningRequests.remove(uri)
            deferred.cancel()
        }
    }

    private fun RequestRecord.complete(bitmap: Bitmap?) {
        deferred.complete(bitmap)
        synchronized(lock) {
            runningRequests.remove(uri)
            if (bitmap != null && caching) {
                cache.put(uri, this)
            }
        }
    }

    private class RequestRecord(
        val uri: Uri,
        val deferred: CompletableDeferred<Bitmap?>,
        @GuardedBy("lock") var caching: Boolean
    )
}
