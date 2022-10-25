/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.intentresolver.chooser;

import android.service.chooser.ChooserTarget;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A TargetInfo for Direct Share. Includes a {@link ChooserTarget} representing the
 * Direct Share deep link into an application.
 */
public abstract class ChooserTargetInfo implements TargetInfo {

    @Override
    public final boolean isChooserTargetInfo() {
        return true;
    }

    @Override
    public ArrayList<DisplayResolveInfo> getAllDisplayTargets() {
        // TODO: consider making this the default behavior for all `TargetInfo` implementations
        // (if it's reasonable for `DisplayResolveInfo.getDisplayResolveInfo()` to return `this`).
        if (getDisplayResolveInfo() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(getDisplayResolveInfo()));
    }

    @Override
    public boolean isSimilar(TargetInfo other) {
        if (other == null) return false;

        ChooserTarget ct1 = getChooserTarget();
        ChooserTarget ct2 = other.getChooserTarget();

        // If either is null, there is not enough info to make an informed decision
        // about equality, so just exit
        if (ct1 == null || ct2 == null) return false;

        if (ct1.getComponentName().equals(ct2.getComponentName())
                && TextUtils.equals(getDisplayLabel(), other.getDisplayLabel())
                && TextUtils.equals(getExtendedInfo(), other.getExtendedInfo())) {
            return true;
        }

        return false;
    }
}
