/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PayloadToggleInteractorTest {
    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)

    @Test
    fun initialState() =
        testScope.runTest {
            val cursorReader = CursorUriReader(createCursor(10), 2, 2) { true }
            val testSubject =
                PayloadToggleInteractor(
                        scope = testScope.backgroundScope,
                        initiallySharedUris = listOf(makeUri(0), makeUri(2), makeUri(5)),
                        focusedUriIdx = 1,
                        mimeTypeClassifier = DefaultMimeTypeClassifier,
                        cursorReaderProvider = { cursorReader },
                        uriMetadataReader = { uri ->
                            FileInfo.Builder(uri)
                                .withMimeType("image/png")
                                .withPreviewUri(uri)
                                .build()
                        },
                        selectionCallback = { null },
                        targetIntentModifier = { Intent(Intent.ACTION_SEND) },
                    )
                    .apply { start() }

            scheduler.runCurrent()

            testSubject.stateFlow.first().let { initialState ->
                assertThat(initialState.items).hasSize(4)
                assertThat(initialState.items.map { it.uri })
                    .containsExactly(*Array<Uri>(4, ::makeUri))
                    .inOrder()
                assertThat(initialState.hasMoreItemsBefore).isFalse()
                assertThat(initialState.hasMoreItemsAfter).isTrue()
                assertThat(initialState.allowSelectionChange).isTrue()
            }

            testSubject.loadMoreNextItems()
            // this one is expected to be deduplicated
            testSubject.loadMoreNextItems()
            scheduler.runCurrent()

            testSubject.stateFlow.first().let { state ->
                assertThat(state.items.map { it.uri })
                    .containsExactly(*Array(6, ::makeUri))
                    .inOrder()
                assertThat(state.hasMoreItemsBefore).isFalse()
                assertThat(state.hasMoreItemsAfter).isTrue()
                assertThat(state.allowSelectionChange).isTrue()
                assertThat(state.items.map { testSubject.selected(it).first() })
                    .containsExactly(true, false, true, false, false, true)
                    .inOrder()
            }
        }
}

private fun createCursor(count: Int): Cursor {
    return MatrixCursor(arrayOf("uri")).apply {
        for (i in 0 until count) {
            addRow(arrayOf(makeUri(i)))
        }
    }
}

private fun makeUri(id: Int) = Uri.parse("content://org.pkg.app/img-$id.png")
