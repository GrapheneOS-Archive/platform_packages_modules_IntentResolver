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

package com.android.intentresolver.v2.ext

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.viewmodel.CreationExtras

/** Adds one or more key-value pairs to the default Args bundle in this extras instance. */
fun CreationExtras.addDefaultArgs(vararg values: Pair<String, Parcelable>): CreationExtras {
    val defaultArgs: Bundle = get(DEFAULT_ARGS_KEY) ?: Bundle()
    for ((key, value) in values) {
        defaultArgs.putParcelable(key, value)
    }
    return this
}
