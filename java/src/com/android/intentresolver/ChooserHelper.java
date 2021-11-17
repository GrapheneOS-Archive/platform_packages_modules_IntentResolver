/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.intentresolver;

import android.app.Activity;
import android.app.ActivityTaskManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;

import androidx.annotation.VisibleForTesting;

/** Utilities for executing the action that the user selected from a Chooser UI. */
public class ChooserHelper {

    private static final String TAG = "ChooserHelper";

    /** Launch the user's selected target. */
    static void onTargetSelected(
            Activity activity,
            Intent chosenIntent,
            Bundle options,
            IBinder permissionToken,
            boolean ignoreTargetSecurity,
            int userId) {

        // We're dispatching intents that might be coming from legacy apps, so
        // (as in com.android.internal.app.ResolverActivity) exempt ourselves from death.
        StrictMode.disableDeathOnFileUriExposure();
        try {
            activity.startActivityAsCaller(
                    chosenIntent, options, permissionToken, ignoreTargetSecurity, userId);
        } finally {
            StrictMode.enableDeathOnFileUriExposure();
        }
    }

    /**
     * Launch a pre-selected target. In the earliest versions of the unbundled chooser, the user
     * has already selected their target from a system-side ChooserActivity UI, and the selection
     * was delegated to the current Activity to dispatch immediately. Unpack the arguments from the
     * Intent that was sent from the system-side ChooserActivity for this kind of delegated
     * dispatch, and launch the user's selected target using the startActivityAsCaller API.
     */
    @VisibleForTesting
    public static void onChoose(Activity activity) {
        final Intent thisIntent = activity.getIntent();
        final Bundle thisExtras = thisIntent.getExtras();
        final Intent chosenIntent = thisIntent.getParcelableExtra(Intent.EXTRA_INTENT);
        final Bundle options = thisIntent.getParcelableExtra(ActivityTaskManager.EXTRA_OPTIONS);
        final IBinder permissionToken =
                thisExtras.getBinder(ActivityTaskManager.EXTRA_PERMISSION_TOKEN);
        final boolean ignoreTargetSecurity =
                thisIntent.getBooleanExtra(ActivityTaskManager.EXTRA_IGNORE_TARGET_SECURITY, false);
        final int userId = thisIntent.getIntExtra(Intent.EXTRA_USER_ID, -1);

        onTargetSelected(
                activity, chosenIntent, options, permissionToken, ignoreTargetSecurity, userId);
    }
}
