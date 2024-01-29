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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ActivityLaunchModule {

    @Provides
    @ActivityScoped
    fun callerInfo(activity: Activity): ActivityLaunch {
        return ActivityLaunch(
            activity.intent,
            activity.launchedFromUid,
            requireNotNull(activity.launchedFromPackage) {
                "activity.launchedFromPackage was null. This is expected to be non-null for " +
                    "any system-signed application!"
            },
            activity.referrer
        )
    }
}
