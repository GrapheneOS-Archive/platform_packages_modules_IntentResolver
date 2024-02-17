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
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.UserHandle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.android.intentresolver.v2.ResolverActivity.PROFILE_WORK
import com.android.intentresolver.v2.shared.model.Profile.Type.WORK
import com.android.intentresolver.v2.ui.model.ActivityLaunch
import com.android.intentresolver.v2.ui.model.ResolverRequest
import com.android.intentresolver.v2.validation.UncaughtException
import com.android.intentresolver.v2.validation.ValidationResultSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

private val targetUri = Uri.parse("content://example.com/123")

private fun createLaunch(
    targetIntent: Intent,
    referrer: Uri? = null,
) =
    ActivityLaunch(
        intent = targetIntent,
        fromUid = 10000,
        fromPackage = "com.android.example",
        referrer = referrer ?: "android-app://com.android.example".toUri()
    )

class ResolverRequestTest {
    @Test
    fun testDefaults() {
        val intent = Intent(ACTION_VIEW).apply { data = targetUri }
        val launch = createLaunch(intent)

        val result = readResolverRequest(launch)
        assertThat(result).isSuccess()
        assertThat(result).findings().isEmpty()
        val value: ResolverRequest = result.getOrThrow()

        assertThat(value.intent.filterEquals(launch.intent)).isTrue()
        assertThat(value.callingUser).isNull()
        assertThat(value.selectedProfile).isNull()
    }

    @Test
    fun testInvalidSelectedProfile() {
        val intent =
            Intent(ACTION_VIEW).apply {
                data = targetUri
                putExtra(EXTRA_SELECTED_PROFILE, -1000)
            }

        val launch = createLaunch(intent)

        val result = readResolverRequest(launch)

        assertThat(result).isFailure()
        assertWithMessage("the first finding")
            .that(result.findings.firstOrNull())
            .isInstanceOf(UncaughtException::class.java)
    }

    @Test
    fun payloadIntents_includesOnlyTarget() {
        val intent2 = Intent(Intent.ACTION_SEND_MULTIPLE)
        val intent1 =
            Intent(Intent.ACTION_SEND).apply {
                putParcelableArrayListExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayListOf(intent2))
            }
        val launch = createLaunch(targetIntent = intent1)

        val result = readResolverRequest(launch)

        // Assert that payloadIntents does NOT include EXTRA_ALTERNATE_INTENTS
        // that is only supported for Chooser and should be not be added here.
        assertThat(result.value?.payloadIntents).containsExactly(intent1)
    }

    @Test
    fun testAllValues() {
        val intent = Intent(ACTION_VIEW).apply { data = Uri.parse("content://example.com/123") }
        val launch = createLaunch(targetIntent = intent)

        launch.intent.putExtras(
            bundleOf(
                EXTRA_CALLING_USER to UserHandle.of(123),
                EXTRA_SELECTED_PROFILE to PROFILE_WORK,
                EXTRA_IS_AUDIO_CAPTURE_DEVICE to true,
            )
        )

        val result = readResolverRequest(launch)

        assertThat(result).value().isNotNull()
        val value: ResolverRequest = result.getOrThrow()

        assertThat(value.intent.filterEquals(launch.intent)).isTrue()
        assertThat(value.isAudioCaptureDevice).isTrue()
        assertThat(value.callingUser).isEqualTo(UserHandle.of(123))
        assertThat(value.selectedProfile).isEqualTo(WORK)
    }
}
