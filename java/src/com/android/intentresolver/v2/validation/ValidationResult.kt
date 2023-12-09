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
package com.android.intentresolver.v2.validation

import android.util.Log

sealed interface ValidationResult<T> {
    val value: T?
    val findings: List<Finding>

    fun isSuccess() = value != null

    fun getOrThrow(): T =
        checkNotNull(value) { "The result was invalid: " + findings.joinToString(separator = "\n") }

    fun <T> reportToLogcat(tag: String) {
        findings.forEach { Log.println(it.logcatPriority, tag, it.toString()) }
    }
}

data class Valid<T>(override val value: T?, override val findings: List<Finding> = emptyList()) :
    ValidationResult<T>

data class Invalid<T>(override val findings: List<Finding>) : ValidationResult<T> {
    override val value: T? = null
}
