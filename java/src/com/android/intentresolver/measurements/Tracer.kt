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

package com.android.intentresolver.measurements

import android.os.Trace
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "Tracer"
private const val SECTION_LAUNCH_TO_SHORTCUT = "launch-to-shortcut"

object Tracer {
    private val launchToFirstShortcut = AtomicLong(-1L)

    fun markLaunched() {
        if (launchToFirstShortcut.compareAndSet(-1, elapsedTimeNow())) {
            Trace.beginAsyncSection(SECTION_LAUNCH_TO_SHORTCUT, 1)
        }
    }

    fun endLaunchToShortcutTrace() {
        val time = elapsedTimeNow()
        val startTime = launchToFirstShortcut.get()
        if (startTime >= 0 && launchToFirstShortcut.compareAndSet(startTime, -1L)) {
            Trace.endAsyncSection(SECTION_LAUNCH_TO_SHORTCUT, 1)
            Log.d(TAG, "stat to first shortcut time: ${time - startTime} ms")
        }
    }

    private fun elapsedTimeNow() = SystemClock.elapsedRealtime()
}
