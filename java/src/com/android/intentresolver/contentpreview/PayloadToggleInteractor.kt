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

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class PayloadToggleInteractor {

    private val storage = MutableStateFlow<Map<Any, Item>>(emptyMap()) // TODO: implement
    private val selectedKeys = MutableStateFlow<Set<Any>>(emptySet())

    val targetPosition: Flow<Int> = flowOf(0) // TODO: implement
    val previewKeys: Flow<List<Any>> = flowOf(emptyList()) // TODO: implement

    fun setSelected(key: Any, isSelected: Boolean) {
        if (isSelected) {
            selectedKeys.update { it + key }
        } else {
            selectedKeys.update { it - key }
        }
    }

    fun selected(key: Any): Flow<Boolean> = previewKeys.map { key in it }

    fun previewInteractor(key: Any) = PayloadTogglePreviewInteractor(key, this)

    fun previewUri(key: Any): Flow<Uri?> = storage.map { it[key]?.previewUri }

    private data class Item(
        val previewUri: Uri?,
    )
}

class PayloadTogglePreviewInteractor(
    private val key: Any,
    private val interactor: PayloadToggleInteractor,
) {
    fun setSelected(selected: Boolean) {
        interactor.setSelected(key, selected)
    }

    val previewUri: Flow<Uri?> = interactor.previewUri(key)
    val selected: Flow<Boolean> = interactor.selected(key)
}
