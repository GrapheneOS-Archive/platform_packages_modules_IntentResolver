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

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import androidx.core.app.AppComponentFactory
import com.android.intentresolver.ApplicationComponentOwner
import javax.inject.Inject
import javax.inject.Provider

/** Provides instances of application components, delegates construction to Dagger. */
class InjectedAppComponentFactory : AppComponentFactory() {

    @set:Inject lateinit var activityComponentBuilder: ActivityComponent.Factory

    @set:Inject
    lateinit var receivers: Map<Class<*>, @JvmSuppressWildcards Provider<BroadcastReceiver>>

    override fun instantiateApplicationCompat(cl: ClassLoader, className: String): Application {
        val app = super.instantiateApplicationCompat(cl, className)
        if (app !is ApplicationComponentOwner) {
            throw RuntimeException("App must be ApplicationComponentOwner")
        }
        app.doWhenApplicationComponentReady { it.inject(this) }
        return app
    }

    override fun instantiateActivityCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): Activity {
        return runCatching {
                val activities = activityComponentBuilder.create().activities()
                instantiate(className, activities)
            }
            .onFailure {
                if (it is UninitializedPropertyAccessException) {
                    // This should never happen but if it did it would cause errors that could
                    // be very difficult to identify, so we log it out of an abundance of
                    // caution.
                    Log.e(TAG, "Tried to instantiate $className before AppComponent", it)
                }
            }
            .getOrNull()
            ?: super.instantiateActivityCompat(cl, className, intent)
    }

    override fun instantiateReceiverCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): BroadcastReceiver {
        return instantiate(className, receivers)
            ?: super.instantiateReceiverCompat(cl, className, intent)
    }

    private fun <T> instantiate(className: String, providers: Map<Class<*>, Provider<T>>): T? {
        return runCatching { providers[Class.forName(className)]?.get() }
            .onFailure {
                if (it is UninitializedPropertyAccessException) {
                    // This should never happen but if it did it would cause errors that could
                    // be very difficult to identify, so we log it out of an abundance of
                    // caution.
                    Log.e(TAG, "Tried to instantiate $className before AppComponent", it)
                }
            }
            .getOrNull()
    }

    companion object {
        private const val TAG = "AppComponentFactory"
    }
}
