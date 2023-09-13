package com.android.intentresolver.inject

import com.android.intentresolver.logging.EventLogImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SingletonModule {

    @Provides @Singleton fun instanceIdSequence() = EventLogImpl.newIdSequence()
}
