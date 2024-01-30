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
import android.content.Intent.ACTION_CHOOSER
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_INTENT
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.android.intentresolver.v2.ui.model.ActivityLaunch
import com.android.intentresolver.v2.ui.model.ChooserRequest
import com.android.intentresolver.v2.validation.RequiredValueMissing
import com.android.intentresolver.v2.validation.ValidationResultSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@Suppress("DEPRECATION")
class ChooserRequestTest {

    val intent = Intent(ACTION_CHOOSER)
    private val mActivityLaunch =
        ActivityLaunch(
            intent,
            fromUid = 10000,
            fromPackage = "com.android.example",
            referrer = "android-app://com.android.example".toUri()
        )

    @Test
    fun missingIntent() {
        val result = readChooserRequest(mActivityLaunch)

        assertThat(result).value().isNull()
        assertThat(result)
            .findings()
            .containsExactly(RequiredValueMissing(EXTRA_INTENT, Intent::class))
    }

    @Test
    fun minimal() {
        intent.putExtras(bundleOf(EXTRA_INTENT to Intent(ACTION_SEND)))

        val result = readChooserRequest(mActivityLaunch)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.launchedFromPackage).isEqualTo(mActivityLaunch.fromPackage)
        assertThat(result).findings().isEmpty()
    }
}
