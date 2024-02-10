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

import android.app.PendingIntent
import android.content.ContentInterface
import android.content.Intent
import android.content.Intent.ACTION_CHOOSER
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_STREAM
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.service.chooser.ChooserAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.intentresolver.any
import com.android.intentresolver.argumentCaptor
import com.android.intentresolver.capture
import com.android.intentresolver.mock
import com.android.intentresolver.whenever
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

// TODO: replace with the new API AdditionalContentContract$MethodNames#ON_SELECTION_CHANGED
private const val MethodName = "onSelectionChanged"

@RunWith(AndroidJUnit4::class)
class SelectionChangeCallbackTest {
    private val uri = Uri.parse("content://org.pkg/content-provider")
    private val chooserIntent = Intent(ACTION_CHOOSER)
    private val contentResolver = mock<ContentInterface>()
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun testCallbackProducesChooserIntentArgument() {
        val a1 =
            ChooserAction.Builder(
                    Icon.createWithContentUri(createUri(10)),
                    "Action 1",
                    PendingIntent.getBroadcast(
                        context,
                        1,
                        Intent("test"),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
        val a2 =
            ChooserAction.Builder(
                    Icon.createWithContentUri(createUri(11)),
                    "Action 2",
                    PendingIntent.getBroadcast(
                        context,
                        1,
                        Intent("test"),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
        whenever(contentResolver.call(any<String>(), any(), any(), any()))
            .thenReturn(
                Bundle().apply { putParcelableArray(EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(a1, a2)) }
            )

        val testSubject = SelectionChangeCallback(uri, chooserIntent, contentResolver)

        val u1 = createUri(1)
        val u2 = createUri(2)
        val targetIntent =
            Intent(ACTION_SEND_MULTIPLE).apply {
                val uris =
                    ArrayList<Uri>().apply {
                        add(u1)
                        add(u2)
                    }
                putExtra(EXTRA_STREAM, uris)
                type = "image/jpg"
            }
        val result = testSubject.onSelectionChanged(targetIntent)
        assertThat(result).isNotNull()
        assertThat(result?.customActions).hasSize(2)
        assertThat(result?.customActions?.get(0)?.icon).isEqualTo(a1.icon)
        assertThat(result?.customActions?.get(0)?.label).isEqualTo(a1.label)
        assertThat(result?.customActions?.get(1)?.icon).isEqualTo(a2.icon)
        assertThat(result?.customActions?.get(1)?.label).isEqualTo(a2.label)

        val authorityCaptor = argumentCaptor<String>()
        val methodCaptor = argumentCaptor<String>()
        val argCaptor = argumentCaptor<String>()
        val extraCaptor = argumentCaptor<Bundle>()
        verify(contentResolver, times(1))
            .call(
                capture(authorityCaptor),
                capture(methodCaptor),
                capture(argCaptor),
                capture(extraCaptor)
            )
        assertThat(authorityCaptor.value).isEqualTo(uri.authority)
        assertThat(methodCaptor.value).isEqualTo(MethodName)
        assertThat(argCaptor.value).isEqualTo(uri.toString())
        val extraBundle = extraCaptor.value
        assertThat(extraBundle).isNotNull()
        val argChooserIntent = extraBundle.getParcelable(EXTRA_INTENT, Intent::class.java)
        assertThat(argChooserIntent).isNotNull()
        assertThat(argChooserIntent?.action).isEqualTo(chooserIntent.action)
        val argTargetIntent = argChooserIntent?.getParcelableExtra(EXTRA_INTENT, Intent::class.java)
        assertThat(argTargetIntent?.action).isEqualTo(targetIntent.action)
        assertThat(argTargetIntent?.getParcelableArrayListExtra(EXTRA_STREAM, Uri::class.java))
            .containsExactly(u1, u2)
            .inOrder()
    }
}

private fun createUri(id: Int) = Uri.parse("content://org.pkg.images/$id.png")
