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

package com.android.intentresolver.contentpreview

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.intentresolver.R.layout.chooser_grid
import com.android.intentresolver.mock
import com.android.intentresolver.whenever
import com.android.intentresolver.widget.ImagePreviewView.TransitionElementStatusCallback
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class UnifiedContentPreviewUiTest {
    private val actionFactory =
        mock<ChooserContentPreviewUi.ActionFactory> {
            whenever(createCustomActions()).thenReturn(emptyList())
        }
    private val imageLoader = mock<ImageLoader>()
    private val headlineGenerator =
        mock<HeadlineGenerator> {
            whenever(getImagesHeadline(anyInt())).thenReturn("Image Headline")
            whenever(getVideosHeadline(anyInt())).thenReturn("Video Headline")
            whenever(getFilesHeadline(anyInt())).thenReturn("Files Headline")
        }

    private val context
        get() = getInstrumentation().getContext()

    @Test
    fun test_displayImagesWithoutUriMetadata_showImagesHeadline() {
        testLoadingHeadline("image/*", files = null)

        verify(headlineGenerator, times(1)).getImagesHeadline(2)
    }

    @Test
    fun test_displayVideosWithoutUriMetadata_showImagesHeadline() {
        testLoadingHeadline("video/*", files = null)

        verify(headlineGenerator, times(1)).getVideosHeadline(2)
    }

    @Test
    fun test_displayDocumentsWithoutUriMetadata_showImagesHeadline() {
        testLoadingHeadline("application/pdf", files = null)

        verify(headlineGenerator, times(1)).getFilesHeadline(2)
    }

    @Test
    fun test_displayMixedContentWithoutUriMetadata_showImagesHeadline() {
        testLoadingHeadline("*/*", files = null)

        verify(headlineGenerator, times(1)).getFilesHeadline(2)
    }

    @Test
    fun test_displayImagesWithUriMetadataSet_showImagesHeadline() {
        val uri = Uri.parse("content://pkg.app/image.png")
        val files =
            listOf(
                FileInfo.Builder(uri).withMimeType("image/png").build(),
                FileInfo.Builder(uri).withMimeType("image/jpeg").build(),
            )
        testLoadingHeadline("image/*", files)

        verify(headlineGenerator, times(1)).getImagesHeadline(2)
    }

    @Test
    fun test_displayVideosWithUriMetadataSet_showImagesHeadline() {
        val uri = Uri.parse("content://pkg.app/image.png")
        val files =
            listOf(
                FileInfo.Builder(uri).withMimeType("video/mp4").build(),
                FileInfo.Builder(uri).withMimeType("video/mp4").build(),
            )
        testLoadingHeadline("video/*", files)

        verify(headlineGenerator, times(1)).getVideosHeadline(2)
    }

    @Test
    fun test_displayImagesAndVideosWithUriMetadataSet_showImagesHeadline() {
        val uri = Uri.parse("content://pkg.app/image.png")
        val files =
            listOf(
                FileInfo.Builder(uri).withMimeType("image/png").build(),
                FileInfo.Builder(uri).withMimeType("video/mp4").build(),
            )
        testLoadingHeadline("*/*", files)

        verify(headlineGenerator, times(1)).getFilesHeadline(2)
    }

    @Test
    fun test_displayDocumentsWithUriMetadataSet_showImagesHeadline() {
        val uri = Uri.parse("content://pkg.app/image.png")
        val files =
            listOf(
                FileInfo.Builder(uri).withMimeType("application/pdf").build(),
                FileInfo.Builder(uri).withMimeType("application/pdf").build(),
            )
        testLoadingHeadline("application/pdf", files)

        verify(headlineGenerator, times(1)).getFilesHeadline(2)
    }

    private fun testLoadingHeadline(intentMimeType: String, files: List<FileInfo>?) {
        val testSubject =
            UnifiedContentPreviewUi(
                /*isSingleImage=*/ false,
                intentMimeType,
                actionFactory,
                imageLoader,
                DefaultMimeTypeClassifier,
                object : TransitionElementStatusCallback {
                    override fun onTransitionElementReady(name: String) = Unit
                    override fun onAllTransitionElementsReady() = Unit
                },
                /*itemCount=*/ 2,
                headlineGenerator
            )
        val layoutInflater = LayoutInflater.from(context)
        val gridLayout = layoutInflater.inflate(chooser_grid, null, false) as ViewGroup

        files?.let(testSubject::setFiles)
        testSubject.display(context.resources, LayoutInflater.from(context), gridLayout)
    }
}
