package com.android.intentresolver.v2.validation

import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

class ValidationResultSubject(metadata: FailureMetadata, private val actual: ValidationResult<*>?) :
    Subject(metadata, actual) {

    fun isSuccess() = check("isSuccess()").that(actual?.isSuccess()).isTrue()
    fun isFailure() = check("isSuccess()").that(actual?.isSuccess()).isFalse()

    fun value(): Subject = check("value").that(actual?.value)

    fun findings(): IterableSubject = check("findings").that(actual?.findings)

    companion object {
        fun assertThat(input: ValidationResult<*>): ValidationResultSubject =
            assertAbout(::ValidationResultSubject).that(input)
    }
}
