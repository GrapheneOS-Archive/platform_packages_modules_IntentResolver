package com.android.intentresolver.inject

import com.android.intentresolver.FeatureFlags
import com.android.intentresolver.FeatureFlagsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object FeatureFlagsModule {

    @Provides fun featureFlags(): FeatureFlags = FeatureFlagsImpl()
}
