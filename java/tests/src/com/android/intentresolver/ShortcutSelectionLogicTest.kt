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

package com.android.intentresolver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.service.chooser.ChooserTarget
import com.android.intentresolver.chooser.ChooserTargetInfo
import com.android.intentresolver.chooser.SelectableTargetInfo.SelectableTargetInfoCommunicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PACKAGE_A = "package.a"
private const val PACKAGE_B = "package.b"
private const val CLASS_NAME = "./MainActivity"

class ShortcutSelectionLogicTest {
    private val packageTargets = HashMap<String, Array<ChooserTarget>>().apply {
        arrayOf(PACKAGE_A, PACKAGE_B).forEach { pkg ->
            // shortcuts in reverse priority order
            val targets = Array(3) { i ->
                createChooserTarget(
                    "Shortcut $i",
                    (i + 1).toFloat() / 10f,
                    ComponentName(pkg, CLASS_NAME),
                    pkg.shortcutId(i),
                )
            }
            this[pkg] = targets
        }
    }

    private operator fun Map<String, Array<ChooserTarget>>.get(pkg: String, idx: Int) =
        this[pkg]?.get(idx) ?: error("missing package $pkg")

    @Test
    fun testAddShortcuts_no_limits() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val sc1 = packageTargets[PACKAGE_A, 0]
        val sc2 = packageTargets[PACKAGE_A, 1]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ false
        )

        val isUpdated = testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.1f,
            /* targets = */ listOf(sc1, sc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )

        assertTrue("Updates are expected", isUpdated)
        assertShortcutsInOrder(
            listOf(sc2, sc1),
            serviceResults,
            "Two shortcuts are expected as we do not apply per-app shortcut limit"
        )
    }

    @Test
    fun testAddShortcuts_same_package_with_per_package_limit() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val sc1 = packageTargets[PACKAGE_A, 0]
        val sc2 = packageTargets[PACKAGE_A, 1]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ true
        )

        val isUpdated = testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.1f,
            /* targets = */ listOf(sc1, sc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )

        assertTrue("Updates are expected", isUpdated)
        assertShortcutsInOrder(
            listOf(sc2),
            serviceResults,
            "One shortcut is expected as we apply per-app shortcut limit"
        )
    }

    @Test
    fun testAddShortcuts_same_package_no_per_app_limit_with_target_limit() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val sc1 = packageTargets[PACKAGE_A, 0]
        val sc2 = packageTargets[PACKAGE_A, 1]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ false
        )

        val isUpdated = testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.1f,
            /* targets = */ listOf(sc1, sc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 1,
            /* serviceTargets = */ serviceResults
        )

        assertTrue("Updates are expected", isUpdated)
        assertShortcutsInOrder(
            listOf(sc2),
            serviceResults,
            "One shortcut is expected as we apply overall shortcut limit"
        )
    }

    @Test
    fun testAddShortcuts_different_packages_with_per_package_limit() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val pkgAsc1 = packageTargets[PACKAGE_A, 0]
        val pkgAsc2 = packageTargets[PACKAGE_A, 1]
        val pkgBsc1 = packageTargets[PACKAGE_B, 0]
        val pkgBsc2 = packageTargets[PACKAGE_B, 1]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ true
        )

        testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.1f,
            /* targets = */ listOf(pkgAsc1, pkgAsc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )
        testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.2f,
            /* targets = */ listOf(pkgBsc1, pkgBsc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )

        assertShortcutsInOrder(
            listOf(pkgBsc2, pkgAsc2),
            serviceResults,
            "Two shortcuts are expected as we apply per-app shortcut limit"
        )
    }

    @Test
    fun testAddShortcuts_pinned_shortcut() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val sc1 = packageTargets[PACKAGE_A, 0]
        val sc2 = packageTargets[PACKAGE_A, 1]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ false
        )

        val isUpdated = testSubject.addServiceResults(
            /* origTarget = */ mock(),
            /* origTargetScore = */ 0.1f,
            /* targets = */ listOf(sc1, sc2),
            /* isShortcutResult = */ true,
            /* directShareToShortcutInfos = */ mapOf(
                sc1 to createShortcutInfo(
                    PACKAGE_A.shortcutId(1),
                    sc1.componentName, 1).apply {
                        addFlags(ShortcutInfo.FLAG_PINNED)
                    }
            ),
            /* userContext = */ mock(),
            /* mSelectableTargetInfoCommunicator = */ mock(),
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )

        assertTrue("Updates are expected", isUpdated)
        assertShortcutsInOrder(
            listOf(sc1, sc2),
            serviceResults,
            "Two shortcuts are expected as we do not apply per-app shortcut limit"
        )
    }

    @Test
    fun test_available_caller_shortcuts_count_is_limited() {
        val serviceResults = ArrayList<ChooserTargetInfo>()
        val sc1 = packageTargets[PACKAGE_A, 0]
        val sc2 = packageTargets[PACKAGE_A, 1]
        val sc3 = packageTargets[PACKAGE_A, 2]
        val testSubject = ShortcutSelectionLogic(
            /* maxShortcutTargetsPerApp = */ 1,
            /* applySharingAppLimits = */ true
        )
        val targetInfoCommunicator = mock<SelectableTargetInfoCommunicator> {
            whenever(targetIntent).thenReturn(Intent())
        }
        val context = mock<Context> {
            whenever(packageManager).thenReturn(mock())
        }

        testSubject.addServiceResults(
            /* origTarget = */ null,
            /* origTargetScore = */ 0f,
            /* targets = */ listOf(sc1, sc2, sc3),
            /* isShortcutResult = */ false /*isShortcutResult*/,
            /* directShareToShortcutInfos = */ emptyMap(),
            /* userContext = */ context,
            /* mSelectableTargetInfoCommunicator = */ targetInfoCommunicator,
            /* maxRankedTargets = */ 4,
            /* serviceTargets = */ serviceResults
        )

        assertShortcutsInOrder(
            listOf(sc3, sc2),
            serviceResults,
            "At most two caller-provided shortcuts are allowed"
        )
    }

    private fun assertShortcutsInOrder(
        expected: List<ChooserTarget>, actual: List<ChooserTargetInfo>, msg: String? = ""
    ) {
        assertEquals(msg, expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals(
                "Unexpected item at position $i",
                expected[i],
                actual[i].chooserTarget
            )
        }
    }

    private fun String.shortcutId(id: Int) = "$this.$id"
}