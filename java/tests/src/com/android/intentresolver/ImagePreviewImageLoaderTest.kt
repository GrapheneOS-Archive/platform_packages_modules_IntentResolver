/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePreviewImageLoaderTest {
    private val imageSize = Size(300, 300)
    private val uriOne = Uri.parse("content://org.package.app/image-1.png")
    private val uriTwo = Uri.parse("content://org.package.app/image-2.png")
    private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val contentResolver = mock<ContentResolver> {
        whenever(loadThumbnail(any(), any(), anyOrNull())).thenReturn(bitmap)
    }
    private val resources = mock<Resources> {
        whenever(getDimensionPixelSize(R.dimen.chooser_preview_image_max_dimen))
            .thenReturn(imageSize.width)
    }
    private val context = mock<Context> {
        whenever(this.resources).thenReturn(this@ImagePreviewImageLoaderTest.resources)
        whenever(this.contentResolver).thenReturn(this@ImagePreviewImageLoaderTest.contentResolver)
    }
    private val scheduler = TestCoroutineScheduler()
    private val lifecycleOwner = TestLifecycleOwner()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)
    private val testSubject = ImagePreviewImageLoader(
        context, lifecycleOwner.lifecycle, 1, dispatcher
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        lifecycleOwner.state = Lifecycle.State.CREATED
    }

    @After
    fun cleanup() {
        lifecycleOwner.state = Lifecycle.State.DESTROYED
        Dispatchers.resetMain()
    }

    @Test
    fun prePopulate_cachesImagesUpToTheCacheSize() = runTest {
        testSubject.prePopulate(listOf(uriOne, uriTwo))

        verify(contentResolver, times(1)).loadThumbnail(uriOne, imageSize, null)
        verify(contentResolver, never()).loadThumbnail(uriTwo, imageSize, null)

        testSubject(uriOne)
        verify(contentResolver, times(1)).loadThumbnail(uriOne, imageSize, null)
    }

    @Test
    fun invoke_returnCachedImageWhenCalledTwice() = runTest {
        testSubject(uriOne)
        testSubject(uriOne)

        verify(contentResolver, times(1)).loadThumbnail(any(), any(), anyOrNull())
    }

    @Test
    fun invoke_whenInstructed_doesNotCache() = runTest {
        testSubject(uriOne, false)
        testSubject(uriOne, false)

        verify(contentResolver, times(2)).loadThumbnail(any(), any(), anyOrNull())
    }

    @Test
    fun invoke_overlappedRequests_Deduplicate() = runTest {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val testSubject = ImagePreviewImageLoader(context, lifecycleOwner.lifecycle, 1, dispatcher)
        coroutineScope {
            launch(start = UNDISPATCHED) {
                testSubject(uriOne, false)
            }
            launch(start = UNDISPATCHED) {
                testSubject(uriOne, false)
            }
            scheduler.advanceUntilIdle()
        }

        verify(contentResolver, times(1)).loadThumbnail(any(), any(), anyOrNull())
    }

    @Test
    fun invoke_oldRecordsEvictedFromTheCache() = runTest {
        testSubject(uriOne)
        testSubject(uriTwo)
        testSubject(uriTwo)
        testSubject(uriOne)

        verify(contentResolver, times(2)).loadThumbnail(uriOne, imageSize, null)
        verify(contentResolver, times(1)).loadThumbnail(uriTwo, imageSize, null)
    }

    @Test
    fun invoke_doNotCacheNulls() = runTest {
        whenever(contentResolver.loadThumbnail(any(), any(), anyOrNull())).thenReturn(null)
        testSubject(uriOne)
        testSubject(uriOne)

        verify(contentResolver, times(2)).loadThumbnail(uriOne, imageSize, null)
    }

    @Test(expected = CancellationException::class)
    fun invoke_onClosedImageLoaderScope_throwsCancellationException() = runTest {
        lifecycleOwner.state = Lifecycle.State.DESTROYED
        testSubject(uriOne)
    }

    @Test(expected = CancellationException::class)
    fun invoke_imageLoaderScopeClosedMidflight_throwsCancellationException() = runTest {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val testSubject = ImagePreviewImageLoader(context, lifecycleOwner.lifecycle, 1, dispatcher)
        coroutineScope {
            val deferred = async(start = UNDISPATCHED) {
                testSubject(uriOne, false)
            }
            lifecycleOwner.state = Lifecycle.State.DESTROYED
            scheduler.advanceUntilIdle()
            deferred.await()
        }
    }

    @Test
    fun invoke_multipleCallsWithDifferentCacheInstructions_cachingPrevails() = runTest {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val testSubject = ImagePreviewImageLoader(context, lifecycleOwner.lifecycle, 1, dispatcher)
        coroutineScope {
            launch(start = UNDISPATCHED) {
                testSubject(uriOne, false)
            }
            launch(start = UNDISPATCHED) {
                testSubject(uriOne, true)
            }
            scheduler.advanceUntilIdle()
        }
        testSubject(uriOne, true)

        verify(contentResolver, times(1)).loadThumbnail(uriOne, imageSize, null)
    }
}
