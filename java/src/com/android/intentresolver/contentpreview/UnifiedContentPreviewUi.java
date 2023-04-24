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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.android.intentresolver.R;
import com.android.intentresolver.widget.ActionRow;
import com.android.intentresolver.widget.ImagePreviewView.TransitionElementStatusCallback;
import com.android.intentresolver.widget.ScrollableImagePreviewView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class UnifiedContentPreviewUi extends ContentPreviewUi {
    private final List<FileInfo> mFiles;
    @Nullable
    private final ChooserContentPreviewUi.ActionFactory mActionFactory;
    private final ImageLoader mImageLoader;
    private final MimeTypeClassifier mTypeClassifier;
    private final TransitionElementStatusCallback mTransitionElementStatusCallback;
    private final HeadlineGenerator mHeadlineGenerator;

    UnifiedContentPreviewUi(
            List<FileInfo> files,
            ChooserContentPreviewUi.ActionFactory actionFactory,
            ImageLoader imageLoader,
            MimeTypeClassifier typeClassifier,
            TransitionElementStatusCallback transitionElementStatusCallback,
            HeadlineGenerator headlineGenerator) {
        mFiles = files;
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
        boolean showImages = !parent.getContext().getResources().getBoolean(R.bool.minimal_preview);

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

        List<ScrollableImagePreviewView.Preview> previews = new ArrayList<>();
        boolean allImages = !mFiles.isEmpty();
        boolean allVideos = !mFiles.isEmpty();
        for (FileInfo fileInfo : mFiles) {
            ScrollableImagePreviewView.PreviewType previewType =
                    getPreviewType(fileInfo.getMimeType());
            allImages = allImages && previewType == ScrollableImagePreviewView.PreviewType.Image;
            allVideos = allVideos && previewType == ScrollableImagePreviewView.PreviewType.Video;

            if (showImages && fileInfo.getPreviewUri() != null) {
                previews.add(new ScrollableImagePreviewView.Preview(
                        previewType,
                        fileInfo.getPreviewUri()));
            }
        }

        if (showImages) {
            imagePreview.setOnNoPreviewCallback(() -> imagePreview.setVisibility(View.GONE));
            imagePreview.setTransitionElementStatusCallback(mTransitionElementStatusCallback);
            imagePreview.setPreviews(
                    previews,
                    mFiles.size() - previews.size(),
                    mImageLoader);
        } else {
            imagePreview.setVisibility(View.GONE);
            mTransitionElementStatusCallback.onAllTransitionElementsReady();
        }

        if (allImages) {
            displayHeadline(
                    contentPreviewLayout, mHeadlineGenerator.getImagesHeadline(mFiles.size()));
        } else if (allVideos) {
            displayHeadline(
                    contentPreviewLayout, mHeadlineGenerator.getVideosHeadline(mFiles.size()));
        } else {
            displayHeadline(
                    contentPreviewLayout, mHeadlineGenerator.getFilesHeadline(mFiles.size()));
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
        if (mFiles.size() == 1 && mTypeClassifier.isImageType(mFiles.get(0).getMimeType())) {
            action = mActionFactory.createEditButton();
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
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
