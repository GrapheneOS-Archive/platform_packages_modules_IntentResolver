package com.android.intentresolver

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import androidx.core.app.AppComponentFactory
import com.android.intentresolver.dagger.ActivitySubComponent
import javax.inject.Inject
import javax.inject.Provider

/** [AppComponentFactory] that performs dagger injection on Android components. */
class IntentResolverAppComponentFactory : AppComponentFactory() {

    @set:Inject lateinit var activitySubComponentBuilder: Provider<ActivitySubComponent.Builder>
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
                val activities = activitySubComponentBuilder.get().build().activities()
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
        private const val TAG = "IRAppComponentFactory"
    }
}
