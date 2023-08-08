package com.android.intentresolver.dagger

import android.app.Activity
import dagger.Subcomponent
import javax.inject.Provider
import javax.inject.Scope

@MustBeDocumented @Retention(AnnotationRetention.RUNTIME) @Scope annotation class ActivityScope

/** Subcomponent for injections across the life of an Activity. */
@ActivityScope
@Subcomponent(modules = [ActivityModule::class, ActivityBinderModule::class])
interface ActivityComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): ActivityComponent
    }

    fun activities(): Map<Class<*>, @JvmSuppressWildcards Provider<Activity>>
}
