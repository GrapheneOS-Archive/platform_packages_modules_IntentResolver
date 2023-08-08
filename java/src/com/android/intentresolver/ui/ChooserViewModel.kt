/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.intentresolver.dagger.qualifiers.ViewModel as ViewModelQualifier
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

const val TAG = "ChooserViewModel"

/** The primary container for ViewModelScope dependencies. */
class ChooserViewModel
@Inject
constructor(
    @ViewModelQualifier val viewModelScope: CoroutineScope,
) : ViewModel()