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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.intentresolver.R
import com.android.intentresolver.mock
import com.android.intentresolver.whenever
import com.android.intentresolver.widget.ActionRow
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextContentPreviewUiTest {
    private val text = "Shared Text"
    private val title = "Preview Title"
    private val lifecycleOwner = TestLifecycleOwner()
    private val actionFactory =
        object : ChooserContentPreviewUi.ActionFactory {
            override fun getEditButtonRunnable(): Runnable? = null
            override fun getCopyButtonRunnable(): Runnable? = null
            override fun createCustomActions(): List<ActionRow.Action> = emptyList()
            override fun getModifyShareAction(): ActionRow.Action? = null
            override fun getExcludeSharedTextAction(): Consumer<Boolean> = Consumer<Boolean> {}
        }
    private val imageLoader = mock<ImageLoader>()
    private val headlineGenerator =
        mock<HeadlineGenerator> { whenever(getTextHeadline(text)).thenReturn(text) }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun test_display_headlineIsDisplayed() {
        val testSubject =
            TextContentPreviewUi(
                lifecycleOwner.lifecycle,
                text,
                title,
                /*previewThumbnail=*/ null,
                actionFactory,
                imageLoader,
                headlineGenerator,
            )
        val layoutInflater = LayoutInflater.from(context)
        val gridLayout = layoutInflater.inflate(R.layout.chooser_grid, null, false) as ViewGroup

        val previewView =
            testSubject.display(
                context.resources,
                layoutInflater,
                gridLayout,
                /*headlineViewParent=*/ null
            )

        assertThat(previewView).isNotNull()
        val headlineView = previewView?.findViewById<TextView>(R.id.headline)
        assertThat(headlineView).isNotNull()
        assertThat(headlineView?.text).isEqualTo(text)
    }
}
