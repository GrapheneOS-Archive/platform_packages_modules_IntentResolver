/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.intentresolver.contentpreview

import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.util.SparseArray

private const val TAG = ContentPreviewUi.TAG

/**
 * A bi-directional cursor reader. Reads URI from the [cursor] starting from the given [startPos],
 * filters items by [predicate].
 */
class CursorUriReader(
    private val cursor: Cursor,
    startPos: Int,
    private val pageSize: Int,
    private val predicate: (Uri) -> Boolean,
) : PayloadToggleInteractor.CursorReader {
    override val count = cursor.count
    // the first position of the next unread page on the right
    private var rightPos = startPos.coerceIn(0, count)
    // the first position of the next from the leftmost unread page on the left
    private var leftPos = rightPos

    override val hasMoreBefore
        get() = leftPos > 0

    override val hasMoreAfter
        get() = rightPos < count

    override fun readPageAfter(): SparseArray<Uri> {
        if (!hasMoreAfter) return SparseArray()
        if (!cursor.moveToPosition(rightPos)) {
            rightPos = count
            Log.w(TAG, "Failed to move the cursor to position $rightPos, stop reading the cursor")
            return SparseArray()
        }
        val result = SparseArray<Uri>(pageSize)
        do {
            cursor
                .getString(0)
                ?.let(Uri::parse)
                ?.takeIf { predicate(it) }
                ?.let { uri -> result.append(rightPos, uri) }
            rightPos++
        } while (result.size() < pageSize && cursor.moveToNext())
        maybeCloseCursor()
        return result
    }

    override fun readPageBefore(): SparseArray<Uri> {
        if (!hasMoreBefore) return SparseArray()
        val startPos = maxOf(0, leftPos - pageSize)
        if (!cursor.moveToPosition(startPos)) {
            leftPos = 0
            Log.w(TAG, "Failed to move the cursor to position $startPos, stop reading cursor")
            return SparseArray()
        }
        val result = SparseArray<Uri>(leftPos - startPos)
        for (pos in startPos ..< leftPos) {
            cursor
                .getString(0)
                ?.let(Uri::parse)
                ?.takeIf { predicate(it) }
                ?.let { uri -> result.append(pos, uri) }
            if (!cursor.moveToNext()) break
        }
        leftPos = startPos
        maybeCloseCursor()
        return result
    }

    private fun maybeCloseCursor() {
        if (!hasMoreBefore && !hasMoreAfter) {
            close()
        }
    }

    override fun close() {
        cursor.close()
    }
}
