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
package com.android.intentresolver.v2.validation.types

import com.android.intentresolver.v2.validation.Finding
import com.android.intentresolver.v2.validation.Importance
import com.android.intentresolver.v2.validation.Importance.CRITICAL
import com.android.intentresolver.v2.validation.Importance.WARNING
import com.android.intentresolver.v2.validation.Invalid
import com.android.intentresolver.v2.validation.Valid
import com.android.intentresolver.v2.validation.ValidationResult
import com.android.intentresolver.v2.validation.Validator

inline fun <reified T : Any> value(key: String): Validator<T> {
    return SimpleValue(key, T::class)
}

inline fun <reified T : Any> array(key: String): Validator<List<T>> {
    return ParceledArray(key, T::class)
}

/**
 * Convenience function to wrap a finding in an appropriate result type.
 *
 * An error [finding] is suppressed when [importance] == [WARNING]
 */
internal fun <T> createResult(importance: Importance, finding: Finding): ValidationResult<T> {
    return when (importance) {
        WARNING -> Valid(null, listOf(finding).filter { it.importance == WARNING })
        CRITICAL -> Invalid(listOf(finding))
    }
}
