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
import android.util.Size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.Consumer

internal class ImagePreviewImageLoader @JvmOverloads constructor(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageLoader {

    override suspend fun invoke(uri: Uri): Bitmap? = loadImageAsync(uri)

    override fun loadImage(uri: Uri, callback: Consumer<Bitmap?>) {
        lifecycle.coroutineScope.launch {
            val image = loadImageAsync(uri)
            if (isActive) {
                callback.accept(image)
            }
        }
    }

    private suspend fun loadImageAsync(uri: Uri): Bitmap? {
        val size = context.resources.getDimensionPixelSize(R.dimen.chooser_preview_image_max_dimen)
        return withContext(dispatcher) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            }.getOrNull()
        }
    }
}
