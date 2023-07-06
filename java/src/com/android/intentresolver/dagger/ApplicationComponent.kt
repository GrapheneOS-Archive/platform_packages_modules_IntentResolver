package com.android.intentresolver.dagger

import android.app.Application
import com.android.intentresolver.IntentResolverAppComponentFactory
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/** Top level component for injections across the life of the process. */
@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }

    fun inject(appComponentFactory: IntentResolverAppComponentFactory)
}
