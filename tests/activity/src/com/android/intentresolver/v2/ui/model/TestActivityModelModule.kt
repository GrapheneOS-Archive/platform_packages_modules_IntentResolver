/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.intentresolver.v2.ui.model

import android.app.Activity
import android.net.Uri
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(components = [ActivityComponent::class], replaces = [ActivityModelModule::class])
class TestActivityModelModule {

    @Provides
    @ActivityScoped
    fun activityModel(activity: Activity): ActivityModel {
        return ActivityModel(
            intent = activity.intent,
            launchedFromUid = LAUNCHED_FROM_UID,
            launchedFromPackage = LAUNCHED_FROM_PACKAGE,
            referrer = REFERRER)
    }

    companion object {
        const val LAUNCHED_FROM_PACKAGE = "example.com"
        const val LAUNCHED_FROM_UID = 1234
        val REFERRER: Uri = Uri.fromParts(ANDROID_APP_SCHEME, LAUNCHED_FROM_PACKAGE, "")
    }
}