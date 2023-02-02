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

package com.android.intentresolver.flags

import com.android.systemui.flags.UnreleasedFlag

// Flag id, name and namespace should be kept in sync with [com.android.systemui.flags.Flags] to
// make the flags available in the flag flipper app (see go/sysui-flags).
object Flags {
    // TODO(b/266983432) Tracking Bug
    @JvmField
    val SHARESHEET_CUSTOM_ACTIONS = unreleasedFlag(1501, "sharesheet_custom_actions", teamfood = true)

    // TODO(b/266982749) Tracking Bug
    @JvmField
    val SHARESHEET_RESELECTION_ACTION = unreleasedFlag(1502, "sharesheet_reselection_action", teamfood = true)

    // TODO(b/266983474) Tracking Bug
    @JvmField
    val SHARESHEET_IMAGE_AND_TEXT_PREVIEW = unreleasedFlag(
        id = 1503, name = "sharesheet_image_text_preview", teamfood = true
    )

    private fun unreleasedFlag(id: Int, name: String, teamfood: Boolean = false) =
        UnreleasedFlag(id, name, "systemui", teamfood)
}
