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

package com.android.intentresolver.util

import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * A test Handler that executes posted [Runnable] immediately regardless of the target time (delay).
 * Does not support messages.
 */
class TestImmediateHandler : Handler(createTestLooper()) {
    override fun sendMessageAtTime(msg: Message, uptimeMillis: Long): Boolean {
        msg.callback.run()
        return true
    }

    companion object {
        private val looperConstructor by lazy {
            Looper::class.java.getDeclaredConstructor(java.lang.Boolean.TYPE).apply {
                isAccessible = true
            }
        }

        private fun createTestLooper(): Looper = looperConstructor.newInstance(true)
    }
}
