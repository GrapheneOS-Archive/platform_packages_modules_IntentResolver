package com.android.intentresolver.dagger

import android.app.Application
import android.content.Context
import com.android.intentresolver.dagger.qualifiers.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Bindings provided to [ApplicationComponent] and children.
 *
 * These are all @Singleton scope, one for the duration of the process.
 */
@Module(
    subcomponents = [ActivityComponent::class, ViewModelComponent::class],
    includes = [ReceiverBinderModule::class, CoroutinesModule::class],
)
interface ApplicationModule {

    companion object {

        @JvmStatic
        @Provides
        @Singleton
        @App
        fun applicationContext(app: Application): Context = app.applicationContext
    }
}
