package com.android.intentresolver.dagger

import android.app.Activity
import dagger.Subcomponent
import javax.inject.Provider

/** Subcomponent for injections across the life of an Activity. */
@ActivityScope
@Subcomponent(modules = [ActivityModule::class])
interface ActivitySubComponent {

    @Subcomponent.Builder
    interface Builder {
        fun build(): ActivitySubComponent
    }

    fun activities(): Map<Class<*>, @JvmSuppressWildcards Provider<Activity>>
}
