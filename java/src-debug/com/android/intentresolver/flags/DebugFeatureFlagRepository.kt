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

import android.util.SparseBooleanArray
import androidx.annotation.GuardedBy
import com.android.systemui.flags.BooleanFlag
import com.android.systemui.flags.FlagManager
import com.android.systemui.flags.ReleasedFlag
import com.android.systemui.flags.UnreleasedFlag
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
internal class DebugFeatureFlagRepository(
    private val flagManager: FlagManager,
    private val deviceConfig: DeviceConfigProxy,
) : FeatureFlagRepository {
    @GuardedBy("self")
    private val cache = SparseBooleanArray()

    override fun isEnabled(flag: UnreleasedFlag): Boolean = isFlagEnabled(flag)

    override fun isEnabled(flag: ReleasedFlag): Boolean = isFlagEnabled(flag)

    private fun isFlagEnabled(flag: BooleanFlag): Boolean {
        synchronized(cache) {
            val idx = cache.indexOfKey(flag.id)
            if (idx >= 0) return cache.valueAt(idx)
        }
        val flagValue = readFlagValue(flag)
        synchronized(cache) {
            val idx = cache.indexOfKey(flag.id)
            // the first read saved in the cache wins
            if (idx >= 0) return cache.valueAt(idx)
            cache.put(flag.id, flagValue)
        }
        return flagValue
    }

    private fun readFlagValue(flag: BooleanFlag): Boolean {
        val localOverride = runCatching {
            flagManager.isEnabled(flag.id)
        }.getOrDefault(null)
        val remoteOverride = deviceConfig.isEnabled(flag)

        // Only check for teamfood if the default is false
        // and there is no server override.
        if (remoteOverride == null
            && !flag.default
            && localOverride == null
            && !flag.isTeamfoodFlag
            && flag.teamfood
        ) {
            return flagManager.isTeamfoodEnabled
        }
        return localOverride ?: remoteOverride ?: flag.default
    }

    companion object {
        // keep in sync with com.android.systemui.flags.Flags
        private const val TEAMFOOD_FLAG_ID = 1

        private val BooleanFlag.isTeamfoodFlag: Boolean
            get() = id == TEAMFOOD_FLAG_ID

        private val FlagManager.isTeamfoodEnabled: Boolean
            get() = runCatching {
                isEnabled(TEAMFOOD_FLAG_ID) ?: false
            }.getOrDefault(false)
    }
}
