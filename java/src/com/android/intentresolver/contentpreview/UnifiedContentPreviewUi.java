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

package com.android.intentresolver.contentpreview;

import static com.android.intentresolver.contentpreview.ContentPreviewType.CONTENT_PREVIEW_IMAGE;

import android.content.res.Resources;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.intentresolver.ImageLoader;
import com.android.intentresolver.R;
import com.android.intentresolver.widget.ActionRow;
import com.android.intentresolver.widget.ImagePreviewView.TransitionElementStatusCallback;
import com.android.intentresolver.widget.ScrollableImagePreviewView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class UnifiedContentPreviewUi extends ContentPreviewUi {
    private final List<FileInfo> mFiles;
    @Nullable
    private final CharSequence mText;
    private final ChooserContentPreviewUi.ActionFactory mActionFactory;
    private final ImageLoader mImageLoader;
    private final MimeTypeClassifier mTypeClassifier;
    private final TransitionElementStatusCallback mTransitionElementStatusCallback;
    private final HeadlineGenerator mHeadlineGenerator;

    UnifiedContentPreviewUi(
            List<FileInfo> files,
            @Nullable CharSequence text,
            ChooserContentPreviewUi.ActionFactory actionFactory,
            ImageLoader imageLoader,
            MimeTypeClassifier typeClassifier,
            TransitionElementStatusCallback transitionElementStatusCallback,
            HeadlineGenerator headlineGenerator) {
        mFiles = files;
        mText = text;
        mActionFactory = actionFactory;
        mImageLoader = imageLoader;
        mTypeClassifier = typeClassifier;
        mTransitionElementStatusCallback = transitionElementStatusCallback;
        mHeadlineGenerator = headlineGenerator;

        mImageLoader.prePopulate(mFiles.stream()
                .map(FileInfo::getPreviewUri)
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    public int getType() {
        return CONTENT_PREVIEW_IMAGE;
    }

    @Override
    public ViewGroup display(Resources resources, LayoutInflater layoutInflater, ViewGroup parent) {
        ViewGroup layout = displayInternal(layoutInflater, parent);
        displayModifyShareAction(layout, mActionFactory);
        return layout;
    }

    private ViewGroup displayInternal(LayoutInflater layoutInflater, ViewGroup parent) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_image, parent, false);
        ScrollableImagePreviewView imagePreview =
                contentPreviewLayout.findViewById(R.id.scrollable_image_preview);
        imagePreview.setOnNoPreviewCallback(() -> imagePreview.setVisibility(View.GONE));

        final ActionRow actionRow =
                contentPreviewLayout.findViewById(com.android.internal.R.id.chooser_action_row);
        actionRow.setActions(
                createActions(
                        createImagePreviewActions(),
                        mActionFactory.createCustomActions()));

        if (mFiles.size() == 0) {
            Log.i(
                    TAG,
                    "Attempted to display image preview area with zero"
                        + " available images detected in EXTRA_STREAM list");
            imagePreview.setVisibility(View.GONE);
            mTransitionElementStatusCallback.onAllTransitionElementsReady();
            return contentPreviewLayout;
        }

        imagePreview.setTransitionElementStatusCallback(mTransitionElementStatusCallback);

        List<ScrollableImagePreviewView.Preview> previews = new ArrayList<>();
        boolean allImages = !mFiles.isEmpty();
        boolean allVideos = !mFiles.isEmpty();
        for (FileInfo fileInfo : mFiles) {
            ScrollableImagePreviewView.PreviewType previewType =
                    getPreviewType(fileInfo.getMimeType());
            allImages = allImages && previewType == ScrollableImagePreviewView.PreviewType.Image;
            allVideos = allVideos && previewType == ScrollableImagePreviewView.PreviewType.Video;

            if (fileInfo.getPreviewUri() != null) {
                previews.add(new ScrollableImagePreviewView.Preview(
                        previewType,
                        fileInfo.getPreviewUri()));
            }
        }
        imagePreview.setPreviews(
                previews,
                mFiles.size() - previews.size(),
                mImageLoader);

        if (!TextUtils.isEmpty(mText) && mFiles.size() == 1 && allImages) {
            setTextInImagePreviewVisibility(contentPreviewLayout, imagePreview, mActionFactory);
            updateTextWithImageHeadline(contentPreviewLayout);
        } else {
            if (allImages) {
                displayHeadline(
                        contentPreviewLayout, mHeadlineGenerator.getImagesHeadline(mFiles.size()));
            } else if (allVideos) {
                displayHeadline(
                        contentPreviewLayout, mHeadlineGenerator.getVideosHeadline(mFiles.size()));
            } else {
                displayHeadline(
                        contentPreviewLayout, mHeadlineGenerator.getItemsHeadline(mFiles.size()));
            }
        }

        return contentPreviewLayout;
    }

    private List<ActionRow.Action> createImagePreviewActions() {
        ArrayList<ActionRow.Action> actions = new ArrayList<>(2);
        //TODO: add copy action;
        ActionRow.Action action = mActionFactory.createNearbyButton();
        if (action != null) {
            actions.add(action);
        }
        action = mActionFactory.createEditButton();
        if (action != null) {
            actions.add(action);
        }
        return actions;
    }

    private void updateTextWithImageHeadline(ViewGroup contentPreview) {
        CheckBox actionView = contentPreview.requireViewById(R.id.include_text_action);
        if (actionView.getVisibility() == View.VISIBLE && actionView.isChecked()) {
            displayHeadline(contentPreview, mHeadlineGenerator.getImageWithTextHeadline(mText));
        } else {
            displayHeadline(
                    contentPreview, mHeadlineGenerator.getImagesHeadline(mFiles.size()));
        }
    }

    private void setTextInImagePreviewVisibility(
            ViewGroup contentPreview,
            ScrollableImagePreviewView imagePreview,
            ChooserContentPreviewUi.ActionFactory actionFactory) {
        final TextView textView = contentPreview
                .requireViewById(com.android.internal.R.id.content_preview_text);
        CheckBox actionView = contentPreview
                .requireViewById(R.id.include_text_action);
        textView.setVisibility(View.VISIBLE);
        boolean isLink = HttpUriMatcher.isHttpUri(mText.toString());
        textView.setAutoLinkMask(isLink ? Linkify.WEB_URLS : 0);
        textView.setText(mText);

        final int[] actionLabels = isLink
                ? new int[] { R.string.include_link, R.string.exclude_link }
                : new int[] { R.string.include_text, R.string.exclude_text };
        final Consumer<Boolean> shareTextAction = actionFactory.getExcludeSharedTextAction();
        actionView.setChecked(true);
        actionView.setText(actionLabels[1]);
        shareTextAction.accept(false);
        actionView.setOnCheckedChangeListener((view, isChecked) -> {
            view.setText(actionLabels[isChecked ? 1 : 0]);
            textView.setEnabled(isChecked);
            if (imagePreview.getVisibility() == View.VISIBLE) {
                // animate only only if we have preview
                TransitionManager.beginDelayedTransition((ViewGroup) textView.getParent());
                textView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            shareTextAction.accept(!isChecked);
            updateTextWithImageHeadline(contentPreview);
        });
        actionView.setVisibility(View.VISIBLE);
    }

    private ScrollableImagePreviewView.PreviewType getPreviewType(String mimeType) {
        if (mTypeClassifier.isImageType(mimeType)) {
            return ScrollableImagePreviewView.PreviewType.Image;
        }
        if (mTypeClassifier.isVideoType(mimeType)) {
            return ScrollableImagePreviewView.PreviewType.Video;
        }
        return ScrollableImagePreviewView.PreviewType.File;
    }
}
