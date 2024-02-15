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

import android.content.ContentInterface
import android.content.Intent
import android.content.Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
import android.content.Intent.EXTRA_INTENT
import android.net.Uri
import android.os.Bundle
import android.service.chooser.AdditionalContentContract.MethodNames.ON_SELECTION_CHANGED
import android.service.chooser.ChooserAction
import com.android.intentresolver.contentpreview.PayloadToggleInteractor.CallbackResult

/**
 * Encapsulates payload change callback invocation to the sharing app; handles callback arguments
 * and result format mapping.
 */
class SelectionChangeCallback(
    private val uri: Uri,
    private val chooserIntent: Intent,
    private val contentResolver: ContentInterface,
) : (Intent) -> CallbackResult? {
    fun onSelectionChanged(targetIntent: Intent): CallbackResult? =
        contentResolver
            .call(
                requireNotNull(uri.authority) { "URI authority can not be null" },
                ON_SELECTION_CHANGED,
                uri.toString(),
                Bundle().apply {
                    putParcelable(
                        EXTRA_INTENT,
                        Intent(chooserIntent).apply { putExtra(EXTRA_INTENT, targetIntent) }
                    )
                }
            )
            ?.let { bundle ->
                val actions =
                    if (bundle.containsKey(EXTRA_CHOOSER_CUSTOM_ACTIONS)) {
                        bundle
                            .getParcelableArray(
                                EXTRA_CHOOSER_CUSTOM_ACTIONS,
                                ChooserAction::class.java
                            )
                            ?.filterNotNull()
                            ?: emptyList()
                    } else {
                        null
                    }
                CallbackResult(actions)
            }

    override fun invoke(targetIntent: Intent) = onSelectionChanged(targetIntent)
}
