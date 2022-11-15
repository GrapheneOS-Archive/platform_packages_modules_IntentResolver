/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.util.Size;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.android.intentresolver.widget.RoundedRectImageView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Delegate to manage deferred resource loads for content preview assets, while
 * implementing Chooser's application logic for determining timeout/success/failure conditions.
 */
public class ChooserContentPreviewCoordinator implements
        ChooserContentPreviewUi.ContentPreviewCoordinator {
    public ChooserContentPreviewCoordinator(
            ExecutorService backgroundExecutor,
            ChooserActivity chooserActivity,
            Runnable onFailCallback,
            Consumer<View> onSingleImageSuccessCallback) {
        this.mBackgroundExecutor = MoreExecutors.listeningDecorator(backgroundExecutor);
        this.mChooserActivity = chooserActivity;
        this.mOnFailCallback = onFailCallback;
        this.mOnSingleImageSuccessCallback = onSingleImageSuccessCallback;

        this.mImageLoadTimeoutMillis =
                chooserActivity.getResources().getInteger(R.integer.config_shortAnimTime);
    }

    @Override
    public void loadUriIntoView(
            final Callable<RoundedRectImageView> deferredImageViewProvider,
            final Uri imageUri,
            final int extraImageCount) {
        final int size = mChooserActivity.getResources().getDimensionPixelSize(
                R.dimen.chooser_preview_image_max_dimen);

        mHandler.postDelayed(this::onWatchdogTimeout, mImageLoadTimeoutMillis);

        ListenableFuture<Bitmap> bitmapFuture = mBackgroundExecutor.submit(
                () -> mChooserActivity.loadThumbnail(imageUri, new Size(size, size)));

        Futures.addCallback(
                bitmapFuture,
                new FutureCallback<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap loadedBitmap) {
                        try {
                            onLoadCompleted(
                                    deferredImageViewProvider.call(),
                                    loadedBitmap,
                                    extraImageCount);
                        } catch (Exception e) { /* unimportant */ }
                    }

                    @Override
                    public void onFailure(Throwable t) {}
                },
                mHandler::post);
    }

    private static final int IMAGE_FADE_IN_MILLIS = 150;

    private final ChooserActivity mChooserActivity;
    private final ListeningExecutorService mBackgroundExecutor;
    private final Runnable mOnFailCallback;
    private final Consumer<View> mOnSingleImageSuccessCallback;
    private final int mImageLoadTimeoutMillis;

    // TODO: this uses a `Handler` because there doesn't seem to be a straightforward way to get a
    // `ScheduledExecutorService` that posts to the UI thread unless we use Dagger. Eventually we'll
    // use Dagger and can inject this as a `@UiThread ScheduledExecutorService`.
    private final Handler mHandler = new Handler();

    private boolean mAtLeastOneLoaded = false;

    @MainThread
    private void onWatchdogTimeout() {
        if (mChooserActivity.isFinishing()) {
            return;
        }

        // If at least one image loads within the timeout period, allow other loads to continue.
        if (!mAtLeastOneLoaded) {
            mOnFailCallback.run();
        }
    }

    @MainThread
    private void onLoadCompleted(
            @Nullable RoundedRectImageView imageView,
            @Nullable Bitmap loadedBitmap,
            int extraImageCount) {
        if (mChooserActivity.isFinishing()) {
            return;
        }

        // TODO: legacy logic didn't handle a possible null view; handle the same as other
        // single-image failures for now (i.e., this is also a factor in the "race" TODO below).
        boolean thisLoadSucceeded = (imageView != null) && (loadedBitmap != null);
        mAtLeastOneLoaded |= thisLoadSucceeded;

        // TODO: this looks like a race condition. We may know that this specific image failed (i.e.
        // it got a null Bitmap), but we'll only report that to the client (thereby failing out our
        // pending loads) if we haven't yet succeeded in loading some other non-null Bitmap. But
        // there could be other pending loads that would've returned non-null within the timeout
        // window, except they end up (effectively) cancelled because this one single-image load
        // "finished" (failed) faster. The outcome of that race may be fairly predictable (since we
        // *might* imagine that the nulls would usually "load" faster?), but it's not guaranteed
        // since the loads are queued in a thread pool (i.e., in parallel). One option for more
        // deterministic behavior: don't signal the failure callback on a single-image load unless
        // there are no other loads currently pending.
        boolean wholeBatchFailed = !mAtLeastOneLoaded;

        if (thisLoadSucceeded) {
            onImageLoadedSuccessfully(loadedBitmap, imageView, extraImageCount);
        } else if (imageView != null) {
            imageView.setVisibility(View.GONE);
        }

        if (wholeBatchFailed) {
            mOnFailCallback.run();
        }
    }

    @MainThread
    private void onImageLoadedSuccessfully(
            @NonNull Bitmap image,
            RoundedRectImageView imageView,
            int extraImageCount) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setAlpha(0.0f);
        imageView.setImageBitmap(image);

        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f);
        fadeAnim.setInterpolator(new DecelerateInterpolator(1.0f));
        fadeAnim.setDuration(IMAGE_FADE_IN_MILLIS);
        fadeAnim.start();

        if (extraImageCount > 0) {
            imageView.setExtraImageCount(extraImageCount);
        }

        mOnSingleImageSuccessCallback.accept(imageView);
    }
}
