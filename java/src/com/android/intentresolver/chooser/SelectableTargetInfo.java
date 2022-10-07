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
import android.app.Activity;
import android.app.prediction.AppTarget;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.chooser.ChooserTarget;
import android.text.SpannableStringBuilder;
import android.util.HashedStringCache;
import android.util.Log;

import com.android.intentresolver.ChooserActivity;
import com.android.intentresolver.ResolverActivity;
import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;

import java.util.ArrayList;
import java.util.List;

/**
 * Live target, currently selectable by the user.
 * @see NotSelectableTargetInfo
 */
public final class SelectableTargetInfo extends ChooserTargetInfo {
    private static final String TAG = "SelectableTargetInfo";

    private static final String HASHED_STRING_CACHE_TAG = "ChooserActivity";  // For legacy reasons.
    private static final int DEFAULT_SALT_EXPIRATION_DAYS = 7;

    private final int mMaxHashSaltDays = DeviceConfig.getInt(
            DeviceConfig.NAMESPACE_SYSTEMUI,
            SystemUiDeviceConfigFlags.HASH_SALT_MAX_DAYS,
            DEFAULT_SALT_EXPIRATION_DAYS);

    @Nullable
    private final DisplayResolveInfo mSourceInfo;
    @Nullable
    private final ResolveInfo mBackupResolveInfo;
    private final Intent mResolvedIntent;
    private final ChooserTarget mChooserTarget;
    private final String mDisplayLabel;
    @Nullable
    private final AppTarget mAppTarget;
    @Nullable
    private final ShortcutInfo mShortcutInfo;

    /**
     * A refinement intent from the caller, if any (see
     * {@link Intent#EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER})
     */
    private final Intent mFillInIntent;

    /**
     * An intent containing referrer URI (see {@link Activity#getReferrer()} (possibly {@code null})
     * in its extended data under the key {@link Intent#EXTRA_REFERRER}.
     */
    private final Intent mReferrerFillInIntent;
    private final int mFillInFlags;
    private final boolean mIsPinned;
    private final float mModifiedScore;

    private Drawable mDisplayIcon;

    /** Create a new {@link TargetInfo} instance representing a selectable target. */
    public static TargetInfo newSelectableTargetInfo(
            @Nullable DisplayResolveInfo sourceInfo,
            @Nullable ResolveInfo backupResolveInfo,
            Intent resolvedIntent,
            ChooserTarget chooserTarget,
            float modifiedScore,
            @Nullable ShortcutInfo shortcutInfo,
            @Nullable AppTarget appTarget,
            Intent referrerFillInIntent) {
        return new SelectableTargetInfo(
                sourceInfo,
                backupResolveInfo,
                resolvedIntent,
                chooserTarget,
                modifiedScore,
                shortcutInfo,
                appTarget,
                referrerFillInIntent);
    }

    private SelectableTargetInfo(
            @Nullable DisplayResolveInfo sourceInfo,
            @Nullable ResolveInfo backupResolveInfo,
            Intent resolvedIntent,
            ChooserTarget chooserTarget,
            float modifiedScore,
            @Nullable ShortcutInfo shortcutInfo,
            @Nullable AppTarget appTarget,
            Intent referrerFillInIntent) {
        mSourceInfo = sourceInfo;
        mChooserTarget = chooserTarget;
        mModifiedScore = modifiedScore;
        mShortcutInfo = shortcutInfo;
        mAppTarget = appTarget;
        mIsPinned = shortcutInfo != null && shortcutInfo.isPinned();
        mBackupResolveInfo = backupResolveInfo;
        mResolvedIntent = resolvedIntent;
        mReferrerFillInIntent = referrerFillInIntent;

        mFillInIntent = null;
        mFillInFlags = 0;

        mDisplayLabel = sanitizeDisplayLabel(chooserTarget.getTitle());
    }

    private SelectableTargetInfo(SelectableTargetInfo other, Intent fillInIntent, int flags) {
        mSourceInfo = other.mSourceInfo;
        mBackupResolveInfo = other.mBackupResolveInfo;
        mResolvedIntent = other.mResolvedIntent;
        mChooserTarget = other.mChooserTarget;
        mShortcutInfo = other.mShortcutInfo;
        mAppTarget = other.mAppTarget;
        mDisplayIcon = other.mDisplayIcon;
        mFillInIntent = fillInIntent;
        mFillInFlags = flags;
        mModifiedScore = other.mModifiedScore;
        mIsPinned = other.mIsPinned;
        mReferrerFillInIntent = other.mReferrerFillInIntent;

        mDisplayLabel = sanitizeDisplayLabel(mChooserTarget.getTitle());
    }

    @Override
    public boolean isSelectableTargetInfo() {
        return true;
    }

    @Override
    public boolean isSuspended() {
        return (mSourceInfo != null) && mSourceInfo.isSuspended();
    }

    @Override
    @Nullable
    public DisplayResolveInfo getDisplayResolveInfo() {
        return mSourceInfo;
    }

    @Override
    public float getModifiedScore() {
        return mModifiedScore;
    }

    @Override
    public Intent getResolvedIntent() {
        return mResolvedIntent;
    }

    @Override
    public ComponentName getResolvedComponentName() {
        if (mSourceInfo != null) {
            return mSourceInfo.getResolvedComponentName();
        } else if (mBackupResolveInfo != null) {
            return new ComponentName(mBackupResolveInfo.activityInfo.packageName,
                    mBackupResolveInfo.activityInfo.name);
        }
        return null;
    }

    @Override
    public ComponentName getChooserTargetComponentName() {
        return mChooserTarget.getComponentName();
    }

    @Nullable
    public Icon getChooserTargetIcon() {
        return mChooserTarget.getIcon();
    }

    private Intent getBaseIntentToSend() {
        Intent result = getResolvedIntent();
        if (result == null) {
            Log.e(TAG, "ChooserTargetInfo: no base intent available to send");
        } else {
            result = new Intent(result);
            if (mFillInIntent != null) {
                result.fillIn(mFillInIntent, mFillInFlags);
            }
            result.fillIn(mReferrerFillInIntent, 0);
        }
        return result;
    }

    @Override
    public boolean start(Activity activity, Bundle options) {
        throw new RuntimeException("ChooserTargets should be started as caller.");
    }

    @Override
    public boolean startAsCaller(ResolverActivity activity, Bundle options, int userId) {
        final Intent intent = getBaseIntentToSend();
        if (intent == null) {
            return false;
        }
        intent.setComponent(mChooserTarget.getComponentName());
        intent.putExtras(mChooserTarget.getIntentExtras());
        TargetInfo.prepareIntentForCrossProfileLaunch(intent, userId);

        // Important: we will ignore the target security checks in ActivityManager
        // if and only if the ChooserTarget's target package is the same package
        // where we got the ChooserTargetService that provided it. This lets a
        // ChooserTargetService provide a non-exported or permission-guarded target
        // to the chooser for the user to pick.
        //
        // If mSourceInfo is null, we got this ChooserTarget from the caller or elsewhere
        // so we'll obey the caller's normal security checks.
        final boolean ignoreTargetSecurity = mSourceInfo != null
                && mSourceInfo.getResolvedComponentName().getPackageName()
                .equals(mChooserTarget.getComponentName().getPackageName());
        activity.startActivityAsCaller(intent, options, ignoreTargetSecurity, userId);
        return true;
    }

    @Override
    public boolean startAsUser(Activity activity, Bundle options, UserHandle user) {
        throw new RuntimeException("ChooserTargets should be started as caller.");
    }

    @Override
    public ResolveInfo getResolveInfo() {
        return mSourceInfo != null ? mSourceInfo.getResolveInfo() : mBackupResolveInfo;
    }

    @Override
    public CharSequence getDisplayLabel() {
        return mDisplayLabel;
    }

    @Override
    public CharSequence getExtendedInfo() {
        // ChooserTargets have badge icons, so we won't show the extended info to disambiguate.
        return null;
    }

    @Override
    public Drawable getDisplayIcon() {
        return mDisplayIcon;
    }

    public void setDisplayIcon(Drawable icon) {
        mDisplayIcon = icon;
    }

    @Override
    @Nullable
    public ShortcutInfo getDirectShareShortcutInfo() {
        return mShortcutInfo;
    }

    @Override
    @Nullable
    public AppTarget getDirectShareAppTarget() {
        return mAppTarget;
    }

    @Override
    public TargetInfo cloneFilledIn(Intent fillInIntent, int flags) {
        return new SelectableTargetInfo(this, fillInIntent, flags);
    }

    @Override
    public List<Intent> getAllSourceIntents() {
        final List<Intent> results = new ArrayList<>();
        if (mSourceInfo != null) {
            // We only queried the service for the first one in our sourceinfo.
            results.add(mSourceInfo.getAllSourceIntents().get(0));
        }
        return results;
    }

    @Override
    public boolean isPinned() {
        return mIsPinned;
    }

    @Override
    public HashedStringCache.HashResult getHashedTargetIdForMetrics(Context context) {
        final String plaintext =
                mChooserTarget.getComponentName().getPackageName()
                + mChooserTarget.getTitle().toString();
        return HashedStringCache.getInstance().hashString(
                context,
                HASHED_STRING_CACHE_TAG,
                plaintext,
                mMaxHashSaltDays);
    }

    private static String sanitizeDisplayLabel(CharSequence label) {
        SpannableStringBuilder sb = new SpannableStringBuilder(label);
        sb.clearSpans();
        return sb.toString();
    }

    // TODO: merge into ChooserListAdapter.ChooserListCommunicator and delete.
    /**
     * Necessary methods to communicate between {@link SelectableTargetInfo}
     * and {@link ResolverActivity} or {@link ChooserActivity}.
     */
    public interface SelectableTargetInfoCommunicator {

        Intent getTargetIntent();

        Intent getReferrerFillInIntent();
    }
}
