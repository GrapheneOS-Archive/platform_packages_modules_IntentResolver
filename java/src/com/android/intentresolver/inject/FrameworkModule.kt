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

package com.android.intentresolver.inject

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutManager
import android.os.UserManager
import android.view.WindowManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

private fun <T> Context.requireSystemService(serviceClass: Class<T>): T {
    return checkNotNull(getSystemService(serviceClass))
}

@Module
@InstallIn(SingletonComponent::class)
object FrameworkModule {

    @Provides
    fun contentResolver(@ApplicationContext ctx: Context) =
        requireNotNull(ctx.contentResolver) { "ContentResolver is expected but missing" }

    @Provides
    fun activityManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(ActivityManager::class.java)

    @Provides
    fun clipboardManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(ClipboardManager::class.java)

    @Provides
    fun devicePolicyManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(DevicePolicyManager::class.java)

    @Provides
    fun launcherApps(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(LauncherApps::class.java)

    @Provides
    fun packageManager(@ApplicationContext ctx: Context) =
        requireNotNull(ctx.packageManager) { "PackageManager is expected but missing" }

    @Provides
    fun shortcutManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(ShortcutManager::class.java)

    @Provides
    fun userManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(UserManager::class.java)

    @Provides
    fun windowManager(@ApplicationContext ctx: Context) =
        ctx.requireSystemService(WindowManager::class.java)
}
