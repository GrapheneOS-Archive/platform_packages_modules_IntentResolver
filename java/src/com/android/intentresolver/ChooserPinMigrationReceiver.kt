/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

/**
 * Broadcast receiver for receiving Chooser pin data from the legacy chooser.
 *
 * Unions the legacy pins with any existing pins. This receiver is protected by the ADD_CHOOSER_PINS
 * permission. The receiver is required to have the RECEIVE_CHOOSER_PIN_MIGRATION to receive the
 * broadcast.
 */
class ChooserPinMigrationReceiver(
        private val pinnedSharedPrefsProvider: (Context) -> SharedPreferences =
                { context -> ChooserActivity.getPinnedSharedPrefs(context) },
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        Log.i(TAG, "Starting migration")

        val prefsEditor = pinnedSharedPrefsProvider.invoke(context).edit()
        bundle.keySet().forEach { key ->
            if(bundle.getBoolean(key)) {
                prefsEditor.putBoolean(key, true)
            }
        }
        prefsEditor.apply()

        Log.i(TAG, "Migration complete")
    }

    companion object {
        private const val TAG = "ChooserPinMigrationReceiver"
    }
}