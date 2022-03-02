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
 */
public class ChooserActivity extends com.android.internal.app.ChooserActivity {
    private static final String TAG = "ChooserActivity";

    private IBinder mPermissionToken;

    @Override
    public boolean startAsCallerImpl(Intent intent, Bundle options, boolean ignoreTargetSecurity,
            int userId) {
        ChooserHelper.onTargetSelected(
                this, intent, options, mPermissionToken, ignoreTargetSecurity, userId);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPermissionToken = getIntent().getExtras().getBinder(
                ActivityTaskManager.EXTRA_PERMISSION_TOKEN);

        if (mPermissionToken != null) {
            super.onCreate(savedInstanceState);
        } else {
            Log.e(TAG, "No permission token to launch activities from chooser");
            super_onCreate(savedInstanceState);  // Skip up to Activity::onCreate().
            finish();
        }
    }
}
