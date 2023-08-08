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

package com.android.intentresolver.dagger

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import java.io.Closeable
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/** Instantiates new ViewModel instances using Dagger. */
class InjectedViewModelFactory(
    private val viewModelComponentBuilder: ViewModelComponent.Builder,
    creationExtras: CreationExtras,
    private val referrer: Uri,
) : ViewModelProvider.Factory {

    private val defaultArgs = creationExtras[DEFAULT_ARGS_KEY] ?: Bundle()

    private fun viewModelScope(viewModelClass: Class<*>) =
        CloseableCoroutineScope(
            SupervisorJob() + CoroutineName(viewModelClass.simpleName) + Dispatchers.Main.immediate
        )

    private fun <T> newViewModel(
        providerMap: Map<Class<*>, Provider<ViewModel>>,
        modelClass: Class<T>
    ): T {
        val provider =
            providerMap[modelClass]
                ?: error(
                    "Unable to create an instance of $modelClass. " +
                        "Does the class have a binding in ViewModelComponent?"
                )
        return modelClass.cast(provider.get())
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModelScope = viewModelScope(modelClass)
        val viewModelComponent =
            viewModelComponentBuilder
                .coroutineScope(viewModelScope)
                .intentExtras(defaultArgs)
                .referrer(referrer)
                .build()
        val viewModel = newViewModel(viewModelComponent.viewModels(), modelClass)
        viewModel.addCloseable(viewModelScope)
        return viewModel
    }
}

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        if (isActive) {
            coroutineContext.cancel()
        }
    }
}
