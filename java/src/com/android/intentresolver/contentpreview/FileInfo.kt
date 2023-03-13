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
package com.android.intentresolver.contentpreview

import android.net.Uri

internal class FileInfo private constructor(
    val uri: Uri,
    val name: String?,
    val previewUri: Uri?,
    val mimeType: String?
) {
    class Builder(val uri: Uri) {
        var name: String = ""
            private set
        var previewUri: Uri? = null
            private set
        var mimeType: String? = null
            private set

        fun withName(name: String): Builder = apply {
            this.name = name
        }

        fun withPreviewUri(uri: Uri?): Builder = apply {
            previewUri = uri
        }

        fun withMimeType(mimeType: String?): Builder = apply {
            this.mimeType = mimeType
        }

        fun build(): FileInfo = FileInfo(uri, name, previewUri, mimeType)
    }
}
