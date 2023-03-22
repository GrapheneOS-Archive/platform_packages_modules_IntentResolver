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

import android.content.Context
import com.android.intentresolver.R
import android.util.PluralsMessageFormatter

private const val PLURALS_COUNT = "count"

/**
 * HeadlineGenerator generates the text to show at the top of the sharesheet as a brief
 * description of the content being shared.
 */
class HeadlineGeneratorImpl(private val context: Context) : HeadlineGenerator {
    override fun getTextHeadline(text: CharSequence): String {
        if (text.toString().isHttpUri()) {
            return context.getString(R.string.sharing_link)
        }
        return context.getString(R.string.sharing_text)
    }

    override fun getImageWithTextHeadline(text: CharSequence): String {
        if (text.toString().isHttpUri()) {
            return context.getString(R.string.sharing_image_with_link)
        }
        return context.getString(R.string.sharing_image_with_text)
    }

    override fun getImagesHeadline(count: Int): String {
        return PluralsMessageFormatter.format(
            context.resources,
            mapOf(PLURALS_COUNT to count),
            R.string.sharing_images
        )
    }

    override fun getVideosHeadline(count: Int): String {
        return PluralsMessageFormatter.format(
            context.resources,
            mapOf(PLURALS_COUNT to count),
            R.string.sharing_videos
        )
    }

    override fun getItemsHeadline(count: Int): String {
        return PluralsMessageFormatter.format(
            context.resources,
            mapOf(PLURALS_COUNT to count),
            R.string.sharing_items
        )
    }
}
