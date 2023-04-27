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

import android.content.ClipDescription
import android.content.ContentInterface
import android.content.Intent
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.net.Uri
import android.provider.DocumentsContract
import com.android.intentresolver.any
import com.android.intentresolver.anyOrNull
import com.android.intentresolver.contentpreview.ChooserContentPreviewUi.ActionFactory
import com.android.intentresolver.contentpreview.ChooserContentPreviewUi.METADATA_COLUMNS
import com.android.intentresolver.mock
import com.android.intentresolver.whenever
import com.android.intentresolver.widget.ActionRow
import com.android.intentresolver.widget.ImagePreviewView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.function.Consumer

private const val PROVIDER_NAME = "org.pkg.app"
class ChooserContentPreviewUiTest {
    private val contentResolver = mock<ContentInterface>()
    private val imageClassifier = MimeTypeClassifier { mimeType ->
        mimeType != null && ClipDescription.compareMimeTypes(mimeType, "image/*")
    }
    private val headlineGenerator = mock<HeadlineGenerator>()
    private val imageLoader = object : ImageLoader {
        override fun loadImage(uri: Uri, callback: Consumer<Bitmap?>) {
            callback.accept(null)
        }
        override fun prePopulate(uris: List<Uri>) = Unit
        override suspend fun invoke(uri: Uri, caching: Boolean): Bitmap? = null
    }
    private val actionFactory = object : ActionFactory {
        override fun createCopyButton() = ActionRow.Action(label = "Copy", icon = null) {}
        override fun createEditButton(): ActionRow.Action? = null
        override fun createCustomActions(): List<ActionRow.Action> = emptyList()
        override fun getModifyShareAction(): ActionRow.Action? = null
        override fun getExcludeSharedTextAction(): Consumer<Boolean> = Consumer<Boolean> {}
    }
    private val transitionCallback = mock<ImagePreviewView.TransitionElementStatusCallback>()

    @Test
    fun test_nonSendIntentAction_useTextPreviewUi() {
        val targetIntent = Intent(Intent.ACTION_VIEW)
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_TEXT)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }

    @Test
    fun test_textMimeType_useTextPreviewUi() {
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Text Extra")
        }
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_TEXT)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }

    @Test
    fun test_singleImageUri_useImagePreviewUi() {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.png")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(uri)).thenReturn("image/png")
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_singleNonImageUriWithoutPreview_useFilePreviewUi() {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }

    @Test
    fun test_singleUriWithFailingGetType_useFilePreviewUi() {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(any()))
            .thenThrow(SecurityException("Test getType() exception"))
        val testSubject = ChooserContentPreviewUi(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionCallback,
                headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }

    @Test
    fun test_singleNonImageUriWithFailingMetadata_useFilePreviewUi() {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(any())).thenReturn("application/pdf")
        whenever(contentResolver.query(any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(SecurityException("Test query() exception"))
        whenever(contentResolver.getStreamTypes(any(), any()))
            .thenThrow(SecurityException("Test getStreamType() exception"))
        val testSubject = ChooserContentPreviewUi(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionCallback,
                headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }

    @Test
    fun test_SingleNonImageUriWithImageTypeInGetStreamTypes_useImagePreviewUi() {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        whenever(contentResolver.getStreamTypes(uri, "*/*"))
                .thenReturn(arrayOf("application/pdf", "image/png"))
        val testSubject = ChooserContentPreviewUi(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionCallback,
                headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
                .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_SingleNonImageUriWithThumbnailFlag_useImagePreviewUi() {
        testMetadataToImagePreview(
            columns = arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            values = arrayOf(
                DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL or
                        DocumentsContract.Document.FLAG_SUPPORTS_METADATA
            )
        )
    }

    @Test
    fun test_SingleNonImageUriWithMetadataIconUri_useImagePreviewUi() {
        testMetadataToImagePreview(
            columns = arrayOf(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
            values = arrayOf("content://$PROVIDER_NAME/test.pdf?thumbnail"),
        )
    }

    private fun testMetadataToImagePreview(columns: Array<String>, values: Array<Any>) {
        val uri = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        whenever(contentResolver.query(uri, METADATA_COLUMNS, null, null))
            .thenReturn(
                MatrixCursor(columns).apply {
                    addRow(values)
                }
            )
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_multipleImageUri_useImagePreviewUi() {
        val uri1 = Uri.parse("content://$PROVIDER_NAME/test.png")
        val uri2 = Uri.parse("content://$PROVIDER_NAME/test.jpg")
        val targetIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(
                Intent.EXTRA_STREAM,
                ArrayList<Uri>().apply {
                    add(uri1)
                    add(uri2)
                }
            )
        }
        whenever(contentResolver.getType(uri1)).thenReturn("image/png")
        whenever(contentResolver.getType(uri2)).thenReturn("image/jpeg")
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_SomeImageUri_useImagePreviewUi() {
        val uri1 = Uri.parse("content://$PROVIDER_NAME/test.png")
        val uri2 = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(
                Intent.EXTRA_STREAM,
                ArrayList<Uri>().apply {
                    add(uri1)
                    add(uri2)
                }
            )
        }
        whenever(contentResolver.getType(uri1)).thenReturn("image/png")
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val testSubject = ChooserContentPreviewUi(
            targetIntent,
            contentResolver,
            imageClassifier,
            imageLoader,
            actionFactory,
            transitionCallback,
            headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
            .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_someNonImageUriWithPreview_useImagePreviewUi() {
        val uri1 = Uri.parse("content://$PROVIDER_NAME/test.mp4")
        val uri2 = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList<Uri>().apply {
                        add(uri1)
                        add(uri2)
                    }
            )
        }
        whenever(contentResolver.getType(uri1)).thenReturn("video/mpeg4")
        whenever(contentResolver.getStreamTypes(uri1, "*/*"))
                .thenReturn(arrayOf("image/png"))
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val testSubject = ChooserContentPreviewUi(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionCallback,
                headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
                .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        verify(transitionCallback, never()).onAllTransitionElementsReady()
    }

    @Test
    fun test_allNonImageUrisWithoutPreview_useFilePreviewUi() {
        val uri1 = Uri.parse("content://$PROVIDER_NAME/test.html")
        val uri2 = Uri.parse("content://$PROVIDER_NAME/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList<Uri>().apply {
                        add(uri1)
                        add(uri2)
                    }
            )
        }
        whenever(contentResolver.getType(uri1)).thenReturn("text/html")
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val testSubject = ChooserContentPreviewUi(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionCallback,
                headlineGenerator
        )
        assertThat(testSubject.preferredContentPreview)
                .isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        verify(transitionCallback, times(1)).onAllTransitionElementsReady()
    }
}
