/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.intentresolver.chooser

import android.app.prediction.AppTarget
import android.app.prediction.AppTargetId
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.service.chooser.ChooserTarget
import androidx.test.platform.app.InstrumentationRegistry
import com.android.intentresolver.createChooserTarget
import com.android.intentresolver.createShortcutInfo
import com.android.intentresolver.mock
import com.android.intentresolver.ResolverDataProvider
import com.android.intentresolver.chooser.SelectableTargetInfo.SelectableTargetInfoCommunicator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetInfoTest {
    private val context = InstrumentationRegistry.getInstrumentation().getContext()

    @Test
    fun testNewEmptyTargetInfo() {
        val info = NotSelectableTargetInfo.newEmptyTargetInfo()
        assertThat(info.isEmptyTargetInfo()).isTrue()
        assertThat(info.isChooserTargetInfo()).isTrue()  // From legacy inheritance model.
        assertThat(info.hasDisplayIcon()).isFalse()
        assertThat(info.getDisplayIcon(context)).isNull()
    }

    @Test
    fun testNewPlaceholderTargetInfo() {
        val info = NotSelectableTargetInfo.newPlaceHolderTargetInfo()
        assertThat(info.isPlaceHolderTargetInfo()).isTrue()
        assertThat(info.isChooserTargetInfo()).isTrue()  // From legacy inheritance model.
        assertThat(info.hasDisplayIcon()).isTrue()
        // TODO: test infrastructure isn't set up to assert anything about the icon itself.
    }

    @Test
    fun testNewSelectableTargetInfo() {
        val displayInfo: DisplayResolveInfo = mock()
        val chooserTarget = createChooserTarget(
            "title", 0.3f, ResolverDataProvider.createComponentName(1), "test_shortcut_id")
        val selectableTargetInfoCommunicator: SelectableTargetInfoCommunicator = mock()
        val shortcutInfo = createShortcutInfo("id", ResolverDataProvider.createComponentName(2), 3)
        val appTarget = AppTarget(
            AppTargetId("id"),
            chooserTarget.getComponentName().getPackageName(),
            chooserTarget.getComponentName().getClassName(),
            UserHandle.CURRENT)

        val targetInfo = SelectableTargetInfo.newSelectableTargetInfo(
            context,
            displayInfo,
            chooserTarget,
            0.1f,
            selectableTargetInfoCommunicator,
            shortcutInfo,
            appTarget)
        assertThat(targetInfo.isSelectableTargetInfo()).isTrue()
        assertThat(targetInfo.isChooserTargetInfo()).isTrue()  // From legacy inheritance model.
        assertThat(targetInfo.getDisplayResolveInfo()).isSameInstanceAs(displayInfo)
        assertThat(targetInfo.getChooserTargetComponentName())
            .isEqualTo(chooserTarget.getComponentName())
        assertThat(targetInfo.getDirectShareShortcutId()).isEqualTo(shortcutInfo.getId())
        assertThat(targetInfo.getDirectShareShortcutInfo()).isSameInstanceAs(shortcutInfo)
        assertThat(targetInfo.getDirectShareAppTarget()).isSameInstanceAs(appTarget)
        // TODO: make more meaningful assertions about the behavior of a selectable target.
    }

    @Test
    fun testNewDisplayResolveInfo() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, "testing intent sending")
        intent.setType("text/plain")

        val resolveInfo = ResolverDataProvider.createResolveInfo(3, 0)

        val targetInfo = DisplayResolveInfo.newDisplayResolveInfo(
            intent,
            resolveInfo,
            "label",
            "extended info",
            intent,
            /* resolveInfoPresentationGetter= */ null)
        assertThat(targetInfo.isDisplayResolveInfo()).isTrue()
        assertThat(targetInfo.isMultiDisplayResolveInfo()).isFalse()
        assertThat(targetInfo.isChooserTargetInfo()).isFalse()
    }

    @Test
    fun testNewMultiDisplayResolveInfo() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, "testing intent sending")
        intent.setType("text/plain")

        val resolveInfo = ResolverDataProvider.createResolveInfo(3, 0)
        val firstTargetInfo = DisplayResolveInfo.newDisplayResolveInfo(
            intent,
            resolveInfo,
            "label 1",
            "extended info 1",
            intent,
            /* resolveInfoPresentationGetter= */ null)
        val secondTargetInfo = DisplayResolveInfo.newDisplayResolveInfo(
            intent,
            resolveInfo,
            "label 2",
            "extended info 2",
            intent,
            /* resolveInfoPresentationGetter= */ null)

        val multiTargetInfo = MultiDisplayResolveInfo.newMultiDisplayResolveInfo(
            listOf(firstTargetInfo, secondTargetInfo))

        assertThat(multiTargetInfo.isMultiDisplayResolveInfo()).isTrue()
        assertThat(multiTargetInfo.isDisplayResolveInfo()).isTrue()  // From legacy inheritance.
        assertThat(multiTargetInfo.isChooserTargetInfo()).isFalse()

        assertThat(multiTargetInfo.getExtendedInfo()).isNull()

        assertThat(multiTargetInfo.getAllDisplayTargets())
                .containsExactly(firstTargetInfo, secondTargetInfo)

        assertThat(multiTargetInfo.hasSelected()).isFalse()
        assertThat(multiTargetInfo.getSelectedTarget()).isNull()

        multiTargetInfo.setSelected(1)

        assertThat(multiTargetInfo.hasSelected()).isTrue()
        assertThat(multiTargetInfo.getSelectedTarget()).isEqualTo(secondTargetInfo)

        // TODO: consider exercising activity-start behavior.
        // TODO: consider exercising DisplayResolveInfo base class behavior.
    }
}
