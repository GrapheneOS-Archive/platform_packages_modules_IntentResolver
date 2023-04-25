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

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import com.android.intentresolver.widget.ActionRow
import com.android.intentresolver.widget.ScrollableImagePreviewView.PreviewType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentPreviewUiTest {
    private class TestablePreview() : ContentPreviewUi() {
        override fun getType() = 0

        override fun display(
            resources: Resources?,
            layoutInflater: LayoutInflater?,
            parent: ViewGroup?
        ): ViewGroup {
            throw IllegalStateException()
        }

        // exposing for testing
        fun makeActions(
            system: List<ActionRow.Action>,
            custom: List<ActionRow.Action>
        ): List<ActionRow.Action> {
            return createActions(system, custom)
        }
    }

    @Test
    fun testPreviewTypes() {
        val typeClassifier = object : MimeTypeClassifier {
            override fun isImageType(type: String?) = (type == "image")
            override fun isVideoType(type: String?) = (type == "video")
        }

        assertThat(ContentPreviewUi.getPreviewType(typeClassifier, "image"))
            .isEqualTo(PreviewType.Image)
        assertThat(ContentPreviewUi.getPreviewType(typeClassifier, "video"))
            .isEqualTo(PreviewType.Video)
        assertThat(ContentPreviewUi.getPreviewType(typeClassifier, "other"))
            .isEqualTo(PreviewType.File)
        assertThat(ContentPreviewUi.getPreviewType(typeClassifier, null))
            .isEqualTo(PreviewType.File)
    }

    @Test
    fun testCreateActions() {
        val preview = TestablePreview()

        val system = listOf(ActionRow.Action(label="system", icon=null) {})
        val custom = listOf(ActionRow.Action(label="custom", icon=null) {})

        assertThat(preview.makeActions(system, custom)).isEqualTo(custom)
        assertThat(preview.makeActions(system, listOf())).isEqualTo(system)
    }
}
