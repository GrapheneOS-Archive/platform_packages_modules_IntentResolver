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

package com.android.intentresolver.v2.ui.model

import android.content.Intent
import android.content.Intent.ACTION_CHOOSER
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import com.android.intentresolver.v2.ext.toParcelAndBack
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class ActivityLaunchTest {

    @Test
    fun testDefaultValues() {
        val input = ActivityLaunch(Intent(ACTION_CHOOSER), 0, null, null)

        val output = input.toParcelAndBack()

        assertEquals(input, output)
    }

    @Test
    fun testCommonValues() {
        val intent = Intent(ACTION_CHOOSER).apply { putExtra(EXTRA_TEXT, "Test") }
        val input =
            ActivityLaunch(intent, 1234, "com.example", Uri.parse("android-app://example.com"))

        val output = input.toParcelAndBack()

        assertEquals(input, output)
    }

    fun assertEquals(expected: ActivityLaunch, actual: ActivityLaunch) {
        // Test fields separately: Intent does not override equals()
        assertWithMessage("%s.filterEquals(%s)", actual.intent, expected.intent)
            .that(actual.intent.filterEquals(expected.intent))
            .isTrue()

        assertWithMessage("actual fromUid is equal to expected")
            .that(actual.fromUid)
            .isEqualTo(expected.fromUid)

        assertWithMessage("actual fromPackage is equal to expected")
            .that(actual.fromPackage)
            .isEqualTo(expected.fromPackage)

        assertWithMessage("actual referrer is equal to expected")
            .that(actual.referrer)
            .isEqualTo(expected.referrer)
    }
}
