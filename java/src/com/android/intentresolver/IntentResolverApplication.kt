package com.android.intentresolver

import android.app.Application
import com.android.intentresolver.dagger.ApplicationComponent
import com.android.intentresolver.dagger.DaggerApplicationComponent

/** [Application] that maintains the [ApplicationComponent]. */
class IntentResolverApplication : Application(), ApplicationComponentOwner {

    private lateinit var applicationComponent: ApplicationComponent

    private val pendingDaggerActions = mutableSetOf<(ApplicationComponent) -> Unit>()

    override fun onCreate() {
        super.onCreate()
        applicationComponent = DaggerApplicationComponent.builder().application(this).build()
        pendingDaggerActions.forEach { it.invoke(applicationComponent) }
        pendingDaggerActions.clear()
    }

    override fun doWhenApplicationComponentReady(action: (ApplicationComponent) -> Unit) {
        if (this::applicationComponent.isInitialized) {
            action.invoke(applicationComponent)
        } else {
            pendingDaggerActions.add(action)
        }
    }
}
