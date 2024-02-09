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

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.android.intentresolver.inject.ChooserServiceFlags
import com.android.intentresolver.v2.ui.model.ActivityLaunch
import com.android.intentresolver.v2.ui.model.ActivityLaunch.Companion.ACTIVITY_LAUNCH_KEY
import com.android.intentresolver.v2.ui.model.ChooserRequest
import com.android.intentresolver.v2.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "ChooserViewModel"

@HiltViewModel
class ChooserViewModel @Inject constructor(args: SavedStateHandle, flags: ChooserServiceFlags) :
    ViewModel() {

    private val mActivityLaunch: ActivityLaunch =
        requireNotNull(args[ACTIVITY_LAUNCH_KEY]) {
            "ActivityLaunch missing in SavedStateHandle! ($ACTIVITY_LAUNCH_KEY)"
        }

    /** The result of reading and validating the inputs provided in savedState. */
    private val status: ValidationResult<ChooserRequest> =
        readChooserRequest(mActivityLaunch, flags)

    val chooserRequest: ChooserRequest by lazy { status.getOrThrow() }

    fun init(): Boolean {
        Log.i(TAG, "viewModel init")
        if (!status.isSuccess()) {
            status.reportToLogcat(TAG)
            return false
        }
        Log.i(TAG, "request = $chooserRequest")
        return true
    }
}
