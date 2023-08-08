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
import com.android.intentresolver.dagger.qualifiers.Referrer
import com.android.intentresolver.dagger.qualifiers.ViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Provider
import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlinx.coroutines.CoroutineScope

@Scope @Retention(RUNTIME) @MustBeDocumented annotation class ViewModelScope

/**
 * Provides dependencies within [ViewModelScope] within a [ViewModel].
 *
 * @see InjectedViewModelFactory
 */
@ViewModelScope
@Subcomponent(modules = [ViewModelModule::class, ViewModelBinderModule::class])
interface ViewModelComponent {

    /**
     * Binds instance values from the creating Activity to make them available for injection within
     * [ViewModelScope].
     */
    @Subcomponent.Builder
    interface Builder {
        @BindsInstance fun intentExtras(@ViewModel intentExtras: Bundle): Builder

        @BindsInstance fun referrer(@Referrer uri: Uri): Builder

        @BindsInstance fun coroutineScope(@ViewModel scope: CoroutineScope): Builder

        fun build(): ViewModelComponent
    }

    fun viewModels(): Map<Class<*>, @JvmSuppressWildcards Provider<androidx.lifecycle.ViewModel>>
}
