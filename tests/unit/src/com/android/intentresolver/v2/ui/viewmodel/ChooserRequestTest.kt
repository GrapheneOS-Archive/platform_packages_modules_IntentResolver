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
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_ALTERNATE_INTENTS
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_REFERRER
import android.net.Uri
import android.service.chooser.Flags
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.android.intentresolver.ContentTypeHint
import com.android.intentresolver.inject.FakeChooserServiceFlags
import com.android.intentresolver.v2.ui.model.ActivityModel
import com.android.intentresolver.v2.ui.model.ChooserRequest
import com.android.intentresolver.v2.validation.RequiredValueMissing
import com.android.intentresolver.v2.validation.ValidationResultSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// TODO: replace with the new API constant, Intent#EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI
private const val EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI =
    "android.intent.extra.CHOOSER_ADDITIONAL_CONTENT_URI"

// TODO: replace with the new API constant, Intent#EXTRA_CHOOSER_FOCUSED_ITEM_POSITION
private const val EXTRA_CHOOSER_FOCUSED_ITEM_POSITION =
    "android.intent.extra.CHOOSER_FOCUSED_ITEM_POSITION"

private fun createActivityModel(
    targetIntent: Intent?,
    referrer: Uri? = null,
    additionalIntents: List<Intent>? = null
) =
    ActivityModel(
        Intent(ACTION_CHOOSER).apply {
            targetIntent?.also { putExtra(EXTRA_INTENT, it) }
            additionalIntents?.also { putExtra(EXTRA_ALTERNATE_INTENTS, it.toTypedArray()) }
        },
        launchedFromUid = 10000,
        launchedFromPackage = "com.android.example",
        referrer = referrer ?: "android-app://com.android.example".toUri()
    )

class ChooserRequestTest {

    private val fakeChooserServiceFlags =
        FakeChooserServiceFlags().apply {
            setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, false)
            setFlag(Flags.FLAG_CHOOSER_ALBUM_TEXT, false)
            setFlag(Flags.FLAG_ENABLE_SHARESHEET_METADATA_EXTRA, false)
        }

    @Test
    fun missingIntent() {
        val launch = createActivityModel(targetIntent = null)
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNull()
        assertThat(result)
            .findings()
            .containsExactly(RequiredValueMissing(EXTRA_INTENT, Intent::class))
    }

    @Test
    fun referrerFillIn() {
        val referrer = Uri.parse("android-app://example.com")
        val launch = createActivityModel(targetIntent = Intent(ACTION_SEND), referrer)
        launch.intent.putExtras(bundleOf(EXTRA_REFERRER to referrer))

        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        val fillIn = result.value?.getReferrerFillInIntent()
        assertThat(fillIn?.hasExtra(EXTRA_REFERRER)).isTrue()
        assertThat(fillIn?.getParcelableExtra(EXTRA_REFERRER, Uri::class.java)).isEqualTo(referrer)
    }

    @Test
    fun referrerPackage_isNullWithNonAppReferrer() {
        val referrer = Uri.parse("http://example.com")
        val intent = Intent().putExtras(bundleOf(EXTRA_INTENT to Intent(ACTION_SEND)))

        val launch = createActivityModel(targetIntent = intent, referrer = referrer)

        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result.value?.referrerPackage).isNull()
    }

    @Test
    fun referrerPackage_fromAppReferrer() {
        val referrer = Uri.parse("android-app://example.com")
        val launch = createActivityModel(targetIntent = Intent(ACTION_SEND), referrer)

        launch.intent.putExtras(bundleOf(EXTRA_REFERRER to referrer))

        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result.value?.referrerPackage).isEqualTo(referrer.authority)
    }

    @Test
    fun payloadIntents_includesTargetThenAdditional() {
        val intent1 = Intent(ACTION_SEND)
        val intent2 = Intent(ACTION_SEND_MULTIPLE)
        val launch = createActivityModel(targetIntent = intent1, additionalIntents = listOf(intent2))
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result.value?.payloadIntents).containsExactly(intent1, intent2)
    }

    @Test
    fun testRequest_withOnlyRequiredValues() {
        val intent = Intent().putExtras(bundleOf(EXTRA_INTENT to Intent(ACTION_SEND)))
        val launch = createActivityModel(targetIntent = intent)
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.launchedFromPackage).isEqualTo(launch.launchedFromPackage)
        assertThat(result).findings().isEmpty()
    }

    @Test
    fun testRequest_actionSendWithAdditionalContentUri() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, true)
        val uri = Uri.parse("content://org.pkg/path")
        val position = 10
        val launch =
            createActivityModel(targetIntent = Intent(ACTION_SEND)).apply {
                intent.putExtra(EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI, uri)
                intent.putExtra(EXTRA_CHOOSER_FOCUSED_ITEM_POSITION, position)
            }
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.additionalContentUri).isEqualTo(uri)
        assertThat(value.focusedItemPosition).isEqualTo(position)
        assertThat(result).findings().isEmpty()
    }

    @Test
    fun testRequest_actionSendWithAdditionalContentUri_parametersIgnoredWhenFlagDisabled() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, false)
        val uri = Uri.parse("content://org.pkg/path")
        val position = 10
        val launch =
            createActivityModel(targetIntent = Intent(ACTION_SEND)).apply {
                intent.putExtra(EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI, uri)
                intent.putExtra(EXTRA_CHOOSER_FOCUSED_ITEM_POSITION, position)
            }
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.additionalContentUri).isNull()
        assertThat(value.focusedItemPosition).isEqualTo(0)
        assertThat(result).findings().isEmpty()
    }

    @Test
    fun testRequest_actionSendWithInvalidAdditionalContentUri() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, true)
        val launch =
            createActivityModel(targetIntent = Intent(ACTION_SEND)).apply {
                intent.putExtra(EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI, "content://org.pkg/path")
                intent.putExtra(EXTRA_CHOOSER_FOCUSED_ITEM_POSITION, "1")
            }
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.additionalContentUri).isNull()
        assertThat(value.focusedItemPosition).isEqualTo(0)
    }

    @Test
    fun testRequest_actionSendWithoutAdditionalContentUri() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, true)
        val launch = createActivityModel(targetIntent = Intent(ACTION_SEND))
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.additionalContentUri).isNull()
        assertThat(value.focusedItemPosition).isEqualTo(0)
    }

    @Test
    fun testRequest_actionViewWithAdditionalContentUri() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_PAYLOAD_TOGGLING, true)
        val uri = Uri.parse("content://org.pkg/path")
        val position = 10
        val launch =
            createActivityModel(targetIntent = Intent(ACTION_VIEW)).apply {
                intent.putExtra(EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI, uri)
                intent.putExtra(EXTRA_CHOOSER_FOCUSED_ITEM_POSITION, position)
            }
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        assertThat(result).value().isNotNull()
        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.additionalContentUri).isNull()
        assertThat(value.focusedItemPosition).isEqualTo(0)
        assertThat(result).findings().isEmpty()
    }

    @Test
    fun testAlbumType() {
        fakeChooserServiceFlags.setFlag(Flags.FLAG_CHOOSER_ALBUM_TEXT, true)
        val launch = createActivityModel(Intent(ACTION_SEND))
        launch.intent.putExtra(
            Intent.EXTRA_CHOOSER_CONTENT_TYPE_HINT,
            Intent.CHOOSER_CONTENT_TYPE_ALBUM
        )

        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        val value: ChooserRequest = result.getOrThrow()
        assertThat(value.contentTypeHint).isEqualTo(ContentTypeHint.ALBUM)
        assertThat(result).findings().isEmpty()
    }

    @Test
    fun metadataText_whenFlagFalse_isNull() {
        // Arrange
        fakeChooserServiceFlags.setFlag(Flags.FLAG_ENABLE_SHARESHEET_METADATA_EXTRA, false)
        val metadataText: CharSequence = "Test metadata text"
        val launch =
            createActivityModel(targetIntent = Intent()).apply {
                intent.putExtra(Intent.EXTRA_METADATA_TEXT, metadataText)
            }

        // Act
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        // Assert
        assertThat(result).value().isNotNull()
        assertThat(result.value?.metadataText).isNull()
    }

    @Test
    fun metadataText_whenFlagTrue_isPassedText() {
        // Arrange
        fakeChooserServiceFlags.setFlag(Flags.FLAG_ENABLE_SHARESHEET_METADATA_EXTRA, true)
        val metadataText: CharSequence = "Test metadata text"
        val launch =
            createActivityModel(targetIntent = Intent()).apply {
                intent.putExtra(Intent.EXTRA_METADATA_TEXT, metadataText)
            }

        // Act
        val result = readChooserRequest(launch, fakeChooserServiceFlags)

        // Assert
        assertThat(result).value().isNotNull()
        assertThat(result.value?.metadataText).isEqualTo(metadataText)
    }
}
