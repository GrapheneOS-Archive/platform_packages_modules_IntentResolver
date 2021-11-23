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

        Intent delegatedIntent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
        if (delegatedIntent == null) {
            Log.e(TAG, "No delegated intent");
            return false;
        }

        if (tryToHandleAsDelegatedSelectionImmediately(delegatedIntent)) {
            return false;
        }

        // This activity was launched from the system-side "springboard" component, and the embedded
        // extra intent is the one that the calling app originally used to launch the system-side
        // component. Treat it as if that's the intent that launched *this* activity directly (as we
        // expect it to be when the unbundling migration is complete). This allows the superclass
        // ChooserActivity implementation to see the same Intent data as it normally would in the
        // system-side implementation.
        setIntent(delegatedIntent);

        return true;
    }

    /**
     * Try to handle the delegated intent in the style of the earlier unbundled implementations,
     * where the user has already selected a target and we're just supposed to dispatch it. Return
     * whether this was an Intent that we were able to handle in this way.
     *
     * TODO: we don't need to continue to support this usage as we make more progress on the
     * unbundling migration, but before we remove it we should double-check that there's no code
     * path that might result in a client seeing the USE_DELEGATE_CHOOSER flag set to true in
     * DisplayResolveInfo even though they decided not to hand off to the unbundled UI at onCreate.
     */
    private boolean tryToHandleAsDelegatedSelectionImmediately(Intent delegatedIntent) {

        if (Intent.ACTION_CHOOSER.equals(delegatedIntent.getAction())) {
            // It looks like we're being invoked for a full chooser, not just the selection.
            return false;
        }

        Log.i(TAG, "Dispatching selection delegated from system chooser");
        ChooserHelper.onChoose(this);
        return true;
    }
}
