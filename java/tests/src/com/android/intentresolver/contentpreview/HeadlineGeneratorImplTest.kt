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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class HeadlineGeneratorImplTest {
    @Test
    fun testHeadlineGeneration() {
        val generator = HeadlineGeneratorImpl(
            InstrumentationRegistry.getInstrumentation().getTargetContext())
        val str = "Some sting"
        val url = "http://www.google.com"

        assertThat(generator.getTextHeadline(str)).isEqualTo("Sharing text")
        assertThat(generator.getTextHeadline(url)).isEqualTo("Sharing link")

        assertThat(generator.getImageWithTextHeadline(str)).isEqualTo("Sharing image with text")
        assertThat(generator.getImageWithTextHeadline(url)).isEqualTo("Sharing image with link")

        assertThat(generator.getImagesHeadline(1)).isEqualTo("Sharing image")
        assertThat(generator.getImagesHeadline(4)).isEqualTo("Sharing 4 images")

        assertThat(generator.getVideosHeadline(1)).isEqualTo("Sharing video")
        assertThat(generator.getVideosHeadline(4)).isEqualTo("Sharing 4 videos")

        assertThat(generator.getItemsHeadline(1)).isEqualTo("Sharing 1 item")
        assertThat(generator.getItemsHeadline(4)).isEqualTo("Sharing 4 items")
    }
}
