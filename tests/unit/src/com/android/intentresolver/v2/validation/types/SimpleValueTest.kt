package com.android.intentresolver.v2.validation.types

import com.android.intentresolver.v2.validation.Importance.CRITICAL
import com.android.intentresolver.v2.validation.RequiredValueMissing
import com.android.intentresolver.v2.validation.ValidationResultSubject.Companion.assertThat
import com.android.intentresolver.v2.validation.ValueIsWrongType
import org.junit.Test

class SimpleValueTest {

    /** Test for validation success when the value is present and the correct type. */
    @Test
    fun present() {
        val keyValidator = SimpleValue("key", expected = Double::class)
        val values = mapOf("key" to Math.PI)

        val result = keyValidator.validate(values::get, CRITICAL)
        assertThat(result).findings().isEmpty()
        assertThat(result).value().isEqualTo(Math.PI)
    }

    /** Test for validation success when the value is present and the correct type. */
    @Test
    fun wrongType() {
        val keyValidator = SimpleValue("key", expected = Double::class)
        val values = mapOf("key" to "Apple Pie")

        val result = keyValidator.validate(values::get, CRITICAL)
        assertThat(result).value().isNull()
        assertThat(result)
            .findings()
            .containsExactly(
                ValueIsWrongType(
                    "key",
                    importance = CRITICAL,
                    actualType = String::class,
                    allowedTypes = listOf(Double::class)
                )
            )
    }

    /** Test the failure result when the value is missing. */
    @Test
    fun missing() {
        val keyValidator = SimpleValue("key", expected = Double::class)

        val result = keyValidator.validate(source = { null }, CRITICAL)

        assertThat(result).value().isNull()
        assertThat(result).findings().containsExactly(RequiredValueMissing("key", Double::class))
    }
}
