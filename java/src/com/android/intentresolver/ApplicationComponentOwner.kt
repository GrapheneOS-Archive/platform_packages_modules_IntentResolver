package com.android.intentresolver

import com.android.intentresolver.dagger.ApplicationComponent

/**
 * Interface that should be implemented by the [Application][android.app.Application] object as the
 * owner of the [ApplicationComponent].
 */
interface ApplicationComponentOwner {
    /**
     * Invokes the given [action] when the [ApplicationComponent] has been created. If it has
     * already been created, then it invokes [action] immediately.
     */
    fun doWhenApplicationComponentReady(action: (ApplicationComponent) -> Unit)
}
