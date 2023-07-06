package com.android.intentresolver.dagger

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/** Injections for the [ApplicationComponent] */
@Module(
    subcomponents = [ActivitySubComponent::class],
    includes = [ReceiverBinderModule::class],
)
abstract class ApplicationModule {

    companion object {
        @Provides
        @Singleton
        @App
        fun provideApplicationContext(application: Application): Context =
            application.applicationContext
    }
}
