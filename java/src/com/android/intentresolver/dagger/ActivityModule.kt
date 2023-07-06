package com.android.intentresolver.dagger

import dagger.Module

/** Injections for the [ActivitySubComponent] */
@Module(includes = [ActivityBinderModule::class]) interface ActivityModule
