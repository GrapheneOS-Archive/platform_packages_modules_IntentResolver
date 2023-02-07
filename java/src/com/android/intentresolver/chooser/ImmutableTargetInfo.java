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

package com.android.intentresolver.chooser;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.prediction.AppTarget;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.HashedStringCache;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.intentresolver.ResolverActivity;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link TargetInfo} with immutable data. Any modifications must be made by
 * creating a new instance (e.g., via {@link ImmutableTargetInfo#toBuilder()}).
 */
public final class ImmutableTargetInfo implements TargetInfo {
    private static final String TAG = "TargetInfo";

    /** Delegate interface to implement {@link TargetInfo#getHashedTargetIdForMetrics()}. */
    public interface TargetHashProvider {
        /** Request a hash for the specified {@code target}. */
        HashedStringCache.HashResult getHashedTargetIdForMetrics(
                TargetInfo target, Context context);
    }

    /** Delegate interface to request that the target be launched by a particular API. */
    public interface TargetActivityStarter {
        /**
         * Request that the delegate use the {@link Activity#startActivity()} API to launch the
         * specified {@code target}.
         *
         * @return true if the target was launched successfully.
         */
        boolean start(TargetInfo target, Activity activity, Bundle options);


        /**
         * Request that the delegate use the {@link Activity#startAsCaller()} API to launch the
         * specified {@code target}.
         *
         * @return true if the target was launched successfully.
         */
        boolean startAsCaller(TargetInfo target, Activity activity, Bundle options, int userId);

        /**
         * Request that the delegate use the {@link Activity#startAsUser()} API to launch the
         * specified {@code target}.
         *
         * @return true if the target was launched successfully.
         */
        boolean startAsUser(TargetInfo target, Activity activity, Bundle options, UserHandle user);
    }

    enum LegacyTargetType {
        NOT_LEGACY_TARGET,
        EMPTY_TARGET_INFO,
        PLACEHOLDER_TARGET_INFO,
        SELECTABLE_TARGET_INFO,
        DISPLAY_RESOLVE_INFO,
        MULTI_DISPLAY_RESOLVE_INFO
    };

    /** Builder API to construct {@code ImmutableTargetInfo} instances. */
    public static class Builder {
        @Nullable
        private ComponentName mResolvedComponentName;

        @Nullable
        private ComponentName mChooserTargetComponentName;

        @Nullable
        private ShortcutInfo mDirectShareShortcutInfo;

        @Nullable
        private AppTarget mDirectShareAppTarget;

        @Nullable
        private DisplayResolveInfo mDisplayResolveInfo;

        @Nullable
        private TargetHashProvider mHashProvider;

        @Nullable
        private Intent mReferrerFillInIntent;

        private Intent mResolvedIntent;
        private Intent mTargetIntent;
        private TargetActivityStarter mActivityStarter;
        private ResolveInfo mResolveInfo;
        private CharSequence mDisplayLabel;
        private CharSequence mExtendedInfo;
        private IconHolder mDisplayIconHolder;
        private List<Intent> mSourceIntents;
        private List<DisplayResolveInfo> mAllDisplayTargets;
        private boolean mIsSuspended;
        private boolean mIsPinned;
        private float mModifiedScore = -0.1f;
        private LegacyTargetType mLegacyType = LegacyTargetType.NOT_LEGACY_TARGET;

        /**
         * Configure an {@link Intent} to be built in to the output target as the resolution for the
         * requested target data.
         */
        public Builder setResolvedIntent(Intent resolvedIntent) {
            mResolvedIntent = resolvedIntent;
            return this;
        }

        /**
         * Configure an {@link Intent} to be built in to the output as the "target intent."
         */
        public Builder setTargetIntent(Intent targetIntent) {
            mTargetIntent = targetIntent;
            return this;
        }

        /**
         * Configure a fill-in intent provided by the referrer to be used in populating the launch
         * intent if the output target is ever selected.
         *
         * @see android.content.Intent#fillIn(Intent, int)
         */
        public Builder setReferrerFillInIntent(@Nullable Intent referrerFillInIntent) {
            mReferrerFillInIntent = referrerFillInIntent;
            return this;
        }

        /**
         * Configure a {@link ComponentName} to be built in to the output target, as the real
         * component we were able to resolve on this device given the available target data.
         */
        public Builder setResolvedComponentName(@Nullable ComponentName resolvedComponentName) {
            mResolvedComponentName = resolvedComponentName;
            return this;
        }

        /**
         * Configure a {@link ComponentName} to be built in to the output target, as the component
         * supposedly associated with a {@link ChooserTarget} from which the builder data is being
         * derived.
         */
        public Builder setChooserTargetComponentName(@Nullable ComponentName componentName) {
            mChooserTargetComponentName = componentName;
            return this;
        }

        /** Configure the {@link TargetActivityStarter} to be built in to the output target. */
        public Builder setActivityStarter(TargetActivityStarter activityStarter) {
            mActivityStarter = activityStarter;
            return this;
        }

        /** Configure the {@link ResolveInfo} to be built in to the output target. */
        public Builder setResolveInfo(ResolveInfo resolveInfo) {
            mResolveInfo = resolveInfo;
            return this;
        }

        /** Configure the display label to be built in to the output target. */
        public Builder setDisplayLabel(CharSequence displayLabel) {
            mDisplayLabel = displayLabel;
            return this;
        }

        /** Configure the extended info to be built in to the output target. */
        public Builder setExtendedInfo(CharSequence extendedInfo) {
            mExtendedInfo = extendedInfo;
            return this;
        }

        /** Configure the {@link IconHolder} to be built in to the output target. */
        public Builder setDisplayIconHolder(IconHolder displayIconHolder) {
            mDisplayIconHolder = displayIconHolder;
            return this;
        }

        /** Configure the list of source intents to be built in to the output target. */
        public Builder setAllSourceIntents(List<Intent> sourceIntents) {
            mSourceIntents = sourceIntents;
            return this;
        }

        /** Configure the list of display targets to be built in to the output target. */
        public Builder setAllDisplayTargets(List<DisplayResolveInfo> targets) {
            mAllDisplayTargets = targets;
            return this;
        }

        /** Configure the is-suspended status to be built in to the output target. */
        public Builder setIsSuspended(boolean isSuspended) {
            mIsSuspended = isSuspended;
            return this;
        }

        /** Configure the is-pinned status to be built in to the output target. */
        public Builder setIsPinned(boolean isPinned) {
            mIsPinned = isPinned;
            return this;
        }

        /** Configure the modified score to be built in to the output target. */
        public Builder setModifiedScore(float modifiedScore) {
            mModifiedScore = modifiedScore;
            return this;
        }

        /** Configure the {@link ShortcutInfo} to be built in to the output target. */
        public Builder setDirectShareShortcutInfo(@Nullable ShortcutInfo shortcutInfo) {
            mDirectShareShortcutInfo = shortcutInfo;
            return this;
        }

        /** Configure the {@link AppTarget} to be built in to the output target. */
        public Builder setDirectShareAppTarget(@Nullable AppTarget appTarget) {
            mDirectShareAppTarget = appTarget;
            return this;
        }

        /** Configure the {@link DisplayResolveInfo} to be built in to the output target. */
        public Builder setDisplayResolveInfo(@Nullable DisplayResolveInfo displayResolveInfo) {
            mDisplayResolveInfo = displayResolveInfo;
            return this;
        }

        /** Configure the {@link TargetHashProvider} to be built in to the output target. */
        public Builder setHashProvider(@Nullable TargetHashProvider hashProvider) {
            mHashProvider = hashProvider;
            return this;
        }

        Builder setLegacyType(LegacyTargetType legacyType) {
            mLegacyType = legacyType;
            return this;
        }

        /**
         * Construct an {@code ImmutableTargetInfo} with the current builder data, where the
         * provided intent is used to fill in missing values from the resolved intent before the
         * target is (potentially) ever launched.
         *
         * @see android.content.Intent#fillIn(Intent, int)
         */
        public ImmutableTargetInfo buildWithFillInIntent(
                @Nullable Intent fillInIntent, int fillInFlags) {
            Intent baseIntentToSend = mResolvedIntent;
            if (baseIntentToSend == null) {
                Log.w(TAG, "No base intent to send");
            } else {
                baseIntentToSend = new Intent(baseIntentToSend);
                if (fillInIntent != null) {
                    baseIntentToSend.fillIn(fillInIntent, fillInFlags);
                }
                if (mReferrerFillInIntent != null) {
                    baseIntentToSend.fillIn(mReferrerFillInIntent, 0);
                }
            }

            return new ImmutableTargetInfo(
                    baseIntentToSend,
                    mResolvedIntent,
                    mTargetIntent,
                    mReferrerFillInIntent,
                    mResolvedComponentName,
                    mChooserTargetComponentName,
                    mActivityStarter,
                    mResolveInfo,
                    mDisplayLabel,
                    mExtendedInfo,
                    mDisplayIconHolder,
                    mSourceIntents,
                    mAllDisplayTargets,
                    mIsSuspended,
                    mIsPinned,
                    mModifiedScore,
                    mDirectShareShortcutInfo,
                    mDirectShareAppTarget,
                    mDisplayResolveInfo,
                    mHashProvider,
                    mLegacyType);
        }

        /** Construct an {@code ImmutableTargetInfo} with the current builder data. */
        public ImmutableTargetInfo build() {
            return buildWithFillInIntent(null, 0);
        }
    }

    @Nullable
    private final Intent mReferrerFillInIntent;

    @Nullable
    private final ComponentName mResolvedComponentName;

    @Nullable
    private final ComponentName mChooserTargetComponentName;

    @Nullable
    private final ShortcutInfo mDirectShareShortcutInfo;

    @Nullable
    private final AppTarget mDirectShareAppTarget;

    @Nullable
    private final DisplayResolveInfo mDisplayResolveInfo;

    @Nullable
    private final TargetHashProvider mHashProvider;

    private final Intent mBaseIntentToSend;
    private final Intent mResolvedIntent;
    private final Intent mTargetIntent;
    private final TargetActivityStarter mActivityStarter;
    private final ResolveInfo mResolveInfo;
    private final CharSequence mDisplayLabel;
    private final CharSequence mExtendedInfo;
    private final IconHolder mDisplayIconHolder;
    private final ImmutableList<Intent> mSourceIntents;
    private final ImmutableList<DisplayResolveInfo> mAllDisplayTargets;
    private final boolean mIsSuspended;
    private final boolean mIsPinned;
    private final float mModifiedScore;
    private final LegacyTargetType mLegacyType;

    /** Construct a {@link Builder}. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** Construct a {@link Builder} pre-initialized to match this target. */
    public Builder toBuilder() {
        return newBuilder()
                .setResolvedIntent(getResolvedIntent())
                .setTargetIntent(getTargetIntent())
                .setReferrerFillInIntent(getReferrerFillInIntent())
                .setResolvedComponentName(getResolvedComponentName())
                .setChooserTargetComponentName(getChooserTargetComponentName())
                .setActivityStarter(mActivityStarter)
                .setResolveInfo(getResolveInfo())
                .setDisplayLabel(getDisplayLabel())
                .setExtendedInfo(getExtendedInfo())
                .setDisplayIconHolder(getDisplayIconHolder())
                .setAllSourceIntents(getAllSourceIntents())
                .setAllDisplayTargets(getAllDisplayTargets())
                .setIsSuspended(isSuspended())
                .setIsPinned(isPinned())
                .setModifiedScore(getModifiedScore())
                .setDirectShareShortcutInfo(getDirectShareShortcutInfo())
                .setDirectShareAppTarget(getDirectShareAppTarget())
                .setDisplayResolveInfo(getDisplayResolveInfo())
                .setHashProvider(getHashProvider())
                .setLegacyType(mLegacyType);
    }

    @VisibleForTesting
    Intent getBaseIntentToSend() {
        return mBaseIntentToSend;
    }

    @Override
    public ImmutableTargetInfo cloneFilledIn(Intent fillInIntent, int flags) {
        return toBuilder().buildWithFillInIntent(fillInIntent, flags);
    }

    @Override
    public Intent getResolvedIntent() {
        return mResolvedIntent;
    }

    @Override
    public Intent getTargetIntent() {
        return mTargetIntent;
    }

    @Nullable
    public Intent getReferrerFillInIntent() {
        return mReferrerFillInIntent;
    }

    @Override
    @Nullable
    public ComponentName getResolvedComponentName() {
        return mResolvedComponentName;
    }

    @Override
    @Nullable
    public ComponentName getChooserTargetComponentName() {
        return mChooserTargetComponentName;
    }

    @Override
    public boolean start(Activity activity, Bundle options) {
        return mActivityStarter.start(this, activity, options);
    }

    @Override
    public boolean startAsCaller(ResolverActivity activity, Bundle options, int userId) {
        return mActivityStarter.startAsCaller(this, activity, options, userId);
    }

    @Override
    public boolean startAsUser(Activity activity, Bundle options, UserHandle user) {
        return mActivityStarter.startAsUser(this, activity, options, user);
    }

    @Override
    public ResolveInfo getResolveInfo() {
        return mResolveInfo;
    }

    @Override
    public CharSequence getDisplayLabel() {
        return mDisplayLabel;
    }

    @Override
    public CharSequence getExtendedInfo() {
        return mExtendedInfo;
    }

    @Override
    public IconHolder getDisplayIconHolder() {
        return mDisplayIconHolder;
    }

    @Override
    public List<Intent> getAllSourceIntents() {
        return mSourceIntents;
    }

    @Override
    public ArrayList<DisplayResolveInfo> getAllDisplayTargets() {
        ArrayList<DisplayResolveInfo> targets = new ArrayList<>();
        targets.addAll(mAllDisplayTargets);
        return targets;
    }

    @Override
    public boolean isSuspended() {
        return mIsSuspended;
    }

    @Override
    public boolean isPinned() {
        return mIsPinned;
    }

    @Override
    public float getModifiedScore() {
        return mModifiedScore;
    }

    @Override
    @Nullable
    public ShortcutInfo getDirectShareShortcutInfo() {
        return mDirectShareShortcutInfo;
    }

    @Override
    @Nullable
    public AppTarget getDirectShareAppTarget() {
        return mDirectShareAppTarget;
    }

    @Override
    @Nullable
    public DisplayResolveInfo getDisplayResolveInfo() {
        return mDisplayResolveInfo;
    }

    @Override
    public HashedStringCache.HashResult getHashedTargetIdForMetrics(Context context) {
        return (mHashProvider == null)
                ? null : mHashProvider.getHashedTargetIdForMetrics(this, context);
    }

    @VisibleForTesting
    @Nullable
    TargetHashProvider getHashProvider() {
        return mHashProvider;
    }

    @Override
    public boolean isEmptyTargetInfo() {
        return mLegacyType == LegacyTargetType.EMPTY_TARGET_INFO;
    }

    @Override
    public boolean isPlaceHolderTargetInfo() {
        return mLegacyType == LegacyTargetType.PLACEHOLDER_TARGET_INFO;
    }

    @Override
    public boolean isNotSelectableTargetInfo() {
        return isEmptyTargetInfo() || isPlaceHolderTargetInfo();
    }

    @Override
    public boolean isSelectableTargetInfo() {
        return mLegacyType == LegacyTargetType.SELECTABLE_TARGET_INFO;
    }

    @Override
    public boolean isChooserTargetInfo() {
        return isNotSelectableTargetInfo() || isSelectableTargetInfo();
    }

    @Override
    public boolean isMultiDisplayResolveInfo() {
        return mLegacyType == LegacyTargetType.MULTI_DISPLAY_RESOLVE_INFO;
    }

    @Override
    public boolean isDisplayResolveInfo() {
        return (mLegacyType == LegacyTargetType.DISPLAY_RESOLVE_INFO)
                || isMultiDisplayResolveInfo();
    }

    private ImmutableTargetInfo(
            Intent baseIntentToSend,
            Intent resolvedIntent,
            Intent targetIntent,
            @Nullable Intent referrerFillInIntent,
            @Nullable ComponentName resolvedComponentName,
            @Nullable ComponentName chooserTargetComponentName,
            TargetActivityStarter activityStarter,
            ResolveInfo resolveInfo,
            CharSequence displayLabel,
            CharSequence extendedInfo,
            IconHolder iconHolder,
            @Nullable List<Intent> sourceIntents,
            @Nullable List<DisplayResolveInfo> allDisplayTargets,
            boolean isSuspended,
            boolean isPinned,
            float modifiedScore,
            @Nullable ShortcutInfo directShareShortcutInfo,
            @Nullable AppTarget directShareAppTarget,
            @Nullable DisplayResolveInfo displayResolveInfo,
            @Nullable TargetHashProvider hashProvider,
            LegacyTargetType legacyType) {
        mBaseIntentToSend = baseIntentToSend;
        mResolvedIntent = resolvedIntent;
        mTargetIntent = targetIntent;
        mReferrerFillInIntent = referrerFillInIntent;
        mResolvedComponentName = resolvedComponentName;
        mChooserTargetComponentName = chooserTargetComponentName;
        mActivityStarter = activityStarter;
        mResolveInfo = resolveInfo;
        mDisplayLabel = displayLabel;
        mExtendedInfo = extendedInfo;
        mDisplayIconHolder = iconHolder;
        mSourceIntents = immutableCopyOrEmpty(sourceIntents);
        mAllDisplayTargets = immutableCopyOrEmpty(allDisplayTargets);
        mIsSuspended = isSuspended;
        mIsPinned = isPinned;
        mModifiedScore = modifiedScore;
        mDirectShareShortcutInfo = directShareShortcutInfo;
        mDirectShareAppTarget = directShareAppTarget;
        mDisplayResolveInfo = displayResolveInfo;
        mHashProvider = hashProvider;
        mLegacyType = legacyType;
    }

    private static <E> ImmutableList<E> immutableCopyOrEmpty(@Nullable List<E> source) {
        return (source == null) ? ImmutableList.of() : ImmutableList.copyOf(source);
    }
}
