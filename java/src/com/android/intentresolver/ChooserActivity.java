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

import android.app.ActivityTaskManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Activity for selecting which application ought to handle an ACTION_SEND intent.
 *
 * TODO: this temporary implementation inherits from the system-side ChooserActivity to avoid
 * duplicating code while we're validating the feasibility of the unbundling plans. Architecturally,
 * this is almost exactly equivalent to the "unbundling phase" in which the chooser UI is
 * implemented in the new project. Once we can verify that this works at (or near) parity, we can
 * copy over the source code of the original ChooserActivity instead of inheriting from it here, and
 * then we'll be able to make divergent changes much more quickly. See TODO comments in this file
 * for notes on performing that refactoring step.
 */
public final class ChooserActivity extends com.android.internal.app.ChooserActivity {
    private static final String TAG = "ChooserActivity";

    private IBinder mPermissionToken;
    private boolean mIsAppPredictionServiceAvailable;

    /* TODO: the first section of this file contains overrides for ChooserActivity methods that need
     * to be implemented differently in the delegated version. When the two classes are merged
     * together, the implementations given here should replace the originals. Rationales for the
     * replacements are provided in implementation comments (which could be removed later). */

    /* The unbundled chooser needs to use the permission-token-based API to start activities. */
    @Override
    public boolean startAsCallerImpl(Intent intent, Bundle options, boolean ignoreTargetSecurity,
            int userId) {
        ChooserHelper.onTargetSelected(
                this, intent, options, mPermissionToken, ignoreTargetSecurity, userId);
        return true;
    }

    /* TODO: the remaining methods below include some implementation details specifically related to
     * the temporary inheritance-based design, which may need to be removed or adapted when the two
     * classes are merged together. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean shouldShowUi = processIntent();
        if (shouldShowUi) {
            super.onCreate(savedInstanceState);
        } else {
            super_onCreate(savedInstanceState);  // Skip up to Activity::onCreate().
            finish();
        }
    }

    @Override
    public boolean isAppPredictionServiceAvailable() {
        return mIsAppPredictionServiceAvailable;
    }

    /**
     * Process the intent that was used to launch the unbundled chooser, and return true if the
     * chooser should continue to initialize as in the full Sharesheet UI, or false if the activity
     * should exit immediately.
     */
    private boolean processIntent() {
        mPermissionToken = getIntent().getExtras().getBinder(
                ActivityTaskManager.EXTRA_PERMISSION_TOKEN);
        mIsAppPredictionServiceAvailable = getIntent().getExtras().getBoolean(
                EXTRA_IS_APP_PREDICTION_SERVICE_AVAILABLE);

        if (mPermissionToken == null) {
            Log.e(TAG, "No permission token to launch activities from chooser");
            return false;
        }

        return true;
    }
}
