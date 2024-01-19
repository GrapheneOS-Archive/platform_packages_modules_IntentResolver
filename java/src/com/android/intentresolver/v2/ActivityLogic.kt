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
package com.android.intentresolver.v2

import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager

/**
 * Logic for IntentResolver Activities. Anything that is not the same across activities (including
 * test activities) should be in this interface. Expect there to be one implementation for each
 * activity, including test activities, but all implementations should delegate to a
 * CommonActivityLogic implementation.
 */
interface ActivityLogic : CommonActivityLogic {
    /** The intent for the target. This will always come before additional targets, if any. */
    val targetIntent: Intent
    /** Custom title to display. */
    val title: CharSequence?
    /** Resource ID for the title to display when there is no custom title. */
    val defaultTitleResId: Int
    /** Intents received to be processed. */
    val initialIntents: List<Intent>?
    /** The intents for potential actual targets. [targetIntent] must be first. */
    val payloadIntents: List<Intent>
}

/**
 * Logic that is common to all IntentResolver activities. Anything that is the same across
 * activities (including test activities), should live here.
 */
interface CommonActivityLogic {
    /** The tag to use when logging. */
    val tag: String
    /** A reference to the activity owning, and used by, this logic. */
    val activity: ComponentActivity
    /** The name of the referring package. */
    val referrerPackageName: String?
    /** User manager system service. */
    val userManager: UserManager
    /** Current [UserHandle]s retrievable by type. */
    val annotatedUserHandles: AnnotatedUserHandles?
    /** Monitors for changes to work profile availability. */
    val workProfileAvailabilityManager: WorkProfileAvailabilityManager
}

/**
 * Concrete implementation of the [CommonActivityLogic] interface meant to be delegated to by
 * [ActivityLogic] implementations. Test implementations of [ActivityLogic] may need to create their
 * own [CommonActivityLogic] implementation.
 */
class CommonActivityLogicImpl(
    override val tag: String,
    override val activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
) : CommonActivityLogic {

    override val referrerPackageName: String? =
        activity.referrer.let {
            if (ANDROID_APP_URI_SCHEME == it?.scheme) {
                it.host
            } else {
                null
            }
        }

    override val userManager: UserManager = activity.getSystemService()!!

    override val annotatedUserHandles: AnnotatedUserHandles? =
        try {
            AnnotatedUserHandles.forShareActivity(activity)
        } catch (e: SecurityException) {
            Log.e(tag, "Request from UID without necessary permissions", e)
            null
        }

    override val workProfileAvailabilityManager =
        WorkProfileAvailabilityManager(
            userManager,
            annotatedUserHandles?.workProfileUserHandle,
            onWorkProfileStatusUpdated,
        )

    companion object {
        private const val ANDROID_APP_URI_SCHEME = "android-app"
    }
}
