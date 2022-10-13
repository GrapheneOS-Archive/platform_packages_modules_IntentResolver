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

import android.annotation.Nullable;
import android.service.chooser.ChooserTarget;
import android.text.TextUtils;

/**
 * A TargetInfo for Direct Share. Includes a {@link ChooserTarget} representing the
 * Direct Share deep link into an application.
 */
public abstract class ChooserTargetInfo implements TargetInfo {
    /**
     * @return the target score, including any Chooser-specific modifications that may have been
     * applied (either overriding by special-case for "non-selectable" targets, or by twiddling the
     * scores of "selectable" targets in {@link ChooserListAdapter}). Higher scores are "better."
     */
    public abstract float getModifiedScore();

    /**
     * @return the {@link ChooserTarget} record that contains additional data about this target, if
     * any. This is only non-null for selectable targets (and probably only Direct Share targets?).
     *
     * @deprecated {@link ChooserTarget} (and any other related {@code ChooserTargetService} APIs)
     * got deprecated as part of sunsetting that old system design, but for historical reasons
     * Chooser continues to shoehorn data from other sources into this representation to maintain
     * compatibility with legacy internal APIs. New clients should avoid taking any further
     * dependencies on the {@link ChooserTarget} type; any data they want to query from those
     * records should instead be pulled up to new query methods directly on this class (or on the
     * root {@link TargetInfo}).
     */
    @Deprecated
    @Nullable
    public abstract ChooserTarget getChooserTarget();

    @Override
    public final boolean isChooserTargetInfo() {
        return true;
    }

    /**
     * Do not label as 'equals', since this doesn't quite work
     * as intended with java 8.
     */
    public boolean isSimilar(ChooserTargetInfo other) {
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
