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


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;

import com.android.intentresolver.ResolverActivity;

import java.util.List;

/**
 * A single target as represented in the chooser.
 */
public interface TargetInfo {
    /**
     * Get the resolved intent that represents this target. Note that this may not be the
     * intent that will be launched by calling one of the <code>start</code> methods provided;
     * this is the intent that will be credited with the launch.
     *
     * @return the resolved intent for this target
     */
    Intent getResolvedIntent();

    /**
     * Get the resolved component name that represents this target. Note that this may not
     * be the component that will be directly launched by calling one of the <code>start</code>
     * methods provided; this is the component that will be credited with the launch.
     *
     * @return the resolved ComponentName for this target
     */
    ComponentName getResolvedComponentName();

    /**
     * Start the activity referenced by this target.
     *
     * @param activity calling Activity performing the launch
     * @param options ActivityOptions bundle
     * @return true if the start completed successfully
     */
    boolean start(Activity activity, Bundle options);

    /**
     * Start the activity referenced by this target as if the ResolverActivity's caller
     * was performing the start operation.
     *
     * @param activity calling Activity (actually) performing the launch
     * @param options ActivityOptions bundle
     * @param userId userId to start as or {@link UserHandle#USER_NULL} for activity's caller
     * @return true if the start completed successfully
     */
    boolean startAsCaller(ResolverActivity activity, Bundle options, int userId);

    /**
     * Start the activity referenced by this target as a given user.
     *
     * @param activity calling activity performing the launch
     * @param options ActivityOptions bundle
     * @param user handle for the user to start the activity as
     * @return true if the start completed successfully
     */
    boolean startAsUser(Activity activity, Bundle options, UserHandle user);

    /**
     * Return the ResolveInfo about how and why this target matched the original query
     * for available targets.
     *
     * @return ResolveInfo representing this target's match
     */
    ResolveInfo getResolveInfo();

    /**
     * Return the human-readable text label for this target.
     *
     * @return user-visible target label
     */
    CharSequence getDisplayLabel();

    /**
     * Return any extended info for this target. This may be used to disambiguate
     * otherwise identical targets.
     *
     * @return human-readable disambig string or null if none present
     */
    CharSequence getExtendedInfo();

    /**
     * @return The drawable that should be used to represent this target including badge
     * @param context
     */
    Drawable getDisplayIcon(Context context);

    /**
     * @return true if display icon is available.
     */
    boolean hasDisplayIcon();
    /**
     * Clone this target with the given fill-in information.
     */
    TargetInfo cloneFilledIn(Intent fillInIntent, int flags);

    /**
     * @return the list of supported source intents deduped against this single target
     */
    List<Intent> getAllSourceIntents();

    /**
     * @return true if this target cannot be selected by the user
     */
    boolean isSuspended();

    /**
     * @return true if this target should be pinned to the front by the request of the user
     */
    boolean isPinned();

    /**
     * @return true if this target represents a legacy {@code ChooserTargetInfo}. These objects were
     * historically documented as representing "[a] TargetInfo for Direct Share." However, not all
     * of these targets are actually *valid* for direct share; e.g. some represent "empty" items
     * (although perhaps only for display in the Direct Share UI?). {@link #getChooserTarget()} will
     * return null for any of these "invalid" items. In even earlier versions, these targets may
     * also have been results from (now-deprecated/unsupported) {@code ChooserTargetService} peers;
     * even though we no longer use these services, we're still shoehorning other target data into
     * the deprecated {@link ChooserTarget} structure for compatibility with some internal APIs.
     * TODO: refactor to clarify the semantics of any target for which this method returns true
     * (e.g., are they characterized by their application in the Direct Share UI?), and to remove
     * the scaffolding that adapts to and from the {@link ChooserTarget} structure. Eventually, we
     * expect to remove this method (and others that strictly indicate legacy subclass roles) in
     * favor of a more semantic design that expresses the purpose and distinctions in those roles.
     */
    default boolean isChooserTargetInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code DisplayResolveInfo}. These objects
     * were historically documented as an augmented "TargetInfo plus additional information needed
     * to render it (such as icon and label) and resolve it to an activity." That description in no
     * way distinguishes from the base {@code TargetInfo} API. At the time of writing, these objects
     * are most-clearly defined by their opposite; this returns true for exactly those instances of
     * {@code TargetInfo} where {@link #isChooserTargetInfo()} returns false (these conditions are
     * complementary because they correspond to the immediate {@code TargetInfo} child types that
     * historically partitioned all concrete {@code TargetInfo} implementations). These may(?)
     * represent any target displayed somewhere other than the Direct Share UI.
     */
    default boolean isDisplayResolveInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code MultiDisplayResolveInfo}. These
     * objects were historically documented as representing "a 'stack' of chooser targets for
     * various activities within the same component." For historical reasons this currently can
     * return true only if {@link #isDisplayResolveInfo()} returns true (because the legacy classes
     * shared an inheritance relationship), but new code should avoid relying on that relationship
     * since these APIs are "in transition."
     */
    default boolean isMultiDisplayResolveInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code SelectableTargetInfo}. Note that this
     * is defined for legacy compatibility and may not conform to other notions of a "selectable"
     * target. For historical reasons, this method and {@link #isNotSelectableTargetInfo()} only
     * partition the {@code TargetInfo} instances for which {@link #isChooserTargetInfo()} returns
     * true; otherwise <em>both</em> methods return false.
     * TODO: define selectability for targets not historically from {@code ChooserTargetInfo},
     * then attempt to replace this with a new method like {@code TargetInfo#isSelectable()} that
     * actually partitions <em>all</em> target types (after updating client usage as needed).
     */
    default boolean isSelectableTargetInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code NotSelectableTargetInfo} (i.e., a
     * target where {@link #isChooserTargetInfo()} is true but {@link #isSelectableTargetInfo()} is
     * false). For more information on how this divides the space of targets, see the Javadoc for
     * {@link #isSelectableTargetInfo()}.
     */
    default boolean isNotSelectableTargetInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code ChooserActivity#EmptyTargetInfo}. Note
     * that this is defined for legacy compatibility and may not conform to other notions of an
     * "empty" target.
     */
    default boolean isEmptyTargetInfo() {
        return false;
    }

    /**
     * @return true if this target represents a legacy {@code ChooserActivity#PlaceHolderTargetInfo}
     * (defined only for compatibility with historic use in {@link ChooserListAdapter}). For
     * historic reasons (owing to a legacy subclass relationship) this can return true only if
     * {@link #isNotSelectableTargetInfo()} also returns true.
     */
    default boolean isPlaceHolderTargetInfo() {
        return false;
    }

    /**
     * @return true if this target should be logged with the "direct_share" metrics category in
     * {@link ResolverActivity#maybeLogCrossProfileTargetLaunch()}. This is defined for legacy
     * compatibility and is <em>not</em> likely to be a good indicator of whether this is actually a
     * "direct share" target (e.g. because it historically also applies to "empty" and "placeholder"
     * targets).
     */
    default boolean isInDirectShareMetricsCategory() {
        return isChooserTargetInfo();
    }

    /**
     * Fix the URIs in {@code intent} if cross-profile sharing is required. This should be called
     * before launching the intent as another user.
     */
    static void prepareIntentForCrossProfileLaunch(Intent intent, int targetUserId) {
        final int currentUserId = UserHandle.myUserId();
        if (targetUserId != currentUserId) {
            intent.fixUris(currentUserId);
        }
    }
}
