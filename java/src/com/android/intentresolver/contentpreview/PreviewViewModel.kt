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

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.intentresolver.ChooserRequestParameters

/** A trivial view model to keep a [PreviewDataProvider] instance over a configuration change */
class PreviewViewModel(private val contentResolver: ContentResolver) : ViewModel() {
    private var previewDataProvider: PreviewDataProvider? = null

    fun createOrReuseProvider(chooserRequest: ChooserRequestParameters): PreviewDataProvider {
        return previewDataProvider
            ?: PreviewDataProvider(chooserRequest.targetIntent, contentResolver).also {
                previewDataProvider = it
            }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T =
                    PreviewViewModel(
                        (checkNotNull(extras[APPLICATION_KEY]) as Context).contentResolver
                    ) as T
            }
    }
}
