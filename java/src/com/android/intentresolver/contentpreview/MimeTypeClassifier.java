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

package com.android.intentresolver.contentpreview;

import android.content.ClipDescription;

import androidx.annotation.Nullable;

/**
 * Testing shim to specify whether a given mime type is considered to be an "image."
 *
 * TODO: move away from {@link ChooserActivityOverrideData} as a model to configure our tests,
 * then migrate {@link com.android.intentresolver.ChooserActivity#isImageType(String)} into this
 * class.
 */
public interface MimeTypeClassifier {
    /** @return whether the specified {@code mimeType} is classified as an "image" type. */
    boolean isImageType(@Nullable String mimeType);

    /** @return whether the specified {@code mimeType} is classified as an "video" type */
    default boolean isVideoType(@Nullable String mimeType) {
        return ClipDescription.compareMimeTypes(mimeType, "video/*");
    }
}
