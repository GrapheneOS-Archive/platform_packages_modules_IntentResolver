/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.intentresolver.v2.ui.viewmodel

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_INTENT
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.android.intentresolver.v2.ui.model.CallerInfo
import com.android.intentresolver.v2.ui.model.ChooserRequest
import com.android.intentresolver.v2.validation.RequiredValueMissing
import com.android.intentresolver.v2.validation.ValidationResultSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@Suppress("DEPRECATION")
class ChooserRequestTest {

    private val callerInfo =
        CallerInfo(
            launchedFromUid = 10000,
            launchedFomPackage = "com.android.example",
            referrer = "android-app://com.android.example".toUri()
        )

    @Test
    fun missingIntent() {
        val args = bundleOf()

        val result = readChooserRequest(callerInfo, args::get)

        assertThat(result).value().isNull()
        assertThat(result)
            .findings()
            .containsExactly(RequiredValueMissing(EXTRA_INTENT, Intent::class))
    }

    @Test
    fun minimal() {
        val args = bundleOf(EXTRA_INTENT to Intent(ACTION_SEND))

        val result = readChooserRequest(callerInfo, args::get)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.launchedFromPackage).isEqualTo(callerInfo.launchedFomPackage)
        assertThat(result).findings().isEmpty()
    }
}
