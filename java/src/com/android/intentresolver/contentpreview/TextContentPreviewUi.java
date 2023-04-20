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

import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.intentresolver.ImageLoader;
import com.android.intentresolver.R;
import com.android.intentresolver.widget.ActionRow;

import java.util.ArrayList;
import java.util.List;

class TextContentPreviewUi extends ContentPreviewUi {
    @Nullable
    private final CharSequence mSharingText;
    @Nullable
    private final CharSequence mPreviewTitle;
    @Nullable
    private final Uri mPreviewThumbnail;
    private final ImageLoader mImageLoader;
    private final ChooserContentPreviewUi.ActionFactory mActionFactory;
    private final HeadlineGenerator mHeadlineGenerator;

    TextContentPreviewUi(
            @Nullable CharSequence sharingText,
            @Nullable CharSequence previewTitle,
            @Nullable Uri previewThumbnail,
            ChooserContentPreviewUi.ActionFactory actionFactory,
            ImageLoader imageLoader,
            HeadlineGenerator headlineGenerator) {
        mSharingText = sharingText;
        mPreviewTitle = previewTitle;
        mPreviewThumbnail = previewThumbnail;
        mImageLoader = imageLoader;
        mActionFactory = actionFactory;
        mHeadlineGenerator = headlineGenerator;
    }

    @Override
    public int getType() {
        return ContentPreviewType.CONTENT_PREVIEW_TEXT;
    }

    @Override
    public ViewGroup display(Resources resources, LayoutInflater layoutInflater, ViewGroup parent) {
        ViewGroup layout = displayInternal(layoutInflater, parent);
        displayModifyShareAction(layout, mActionFactory);
        return layout;
    }

    private ViewGroup displayInternal(
            LayoutInflater layoutInflater,
            ViewGroup parent) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_text, parent, false);

        final ActionRow actionRow =
                contentPreviewLayout.findViewById(com.android.internal.R.id.chooser_action_row);
        actionRow.setActions(
                createActions(
                        createTextPreviewActions(),
                        mActionFactory.createCustomActions()));

        if (mSharingText == null) {
            contentPreviewLayout
                    .findViewById(R.id.text_preview_layout)
                    .setVisibility(View.GONE);
            return contentPreviewLayout;
        }

        TextView textView = contentPreviewLayout.findViewById(
                com.android.internal.R.id.content_preview_text);
        textView.setText(mSharingText);

        TextView previewTitleView = contentPreviewLayout.findViewById(
                com.android.internal.R.id.content_preview_title);
        if (TextUtils.isEmpty(mPreviewTitle)) {
            previewTitleView.setVisibility(View.GONE);
        } else {
            previewTitleView.setText(mPreviewTitle);
        }

        ImageView previewThumbnailView = contentPreviewLayout.findViewById(
                com.android.internal.R.id.content_preview_thumbnail);
        if (!validForContentPreview(mPreviewThumbnail)) {
            previewThumbnailView.setVisibility(View.GONE);
        } else {
            mImageLoader.loadImage(
                    mPreviewThumbnail,
                    (bitmap) -> updateViewWithImage(
                            contentPreviewLayout.findViewById(
                                    com.android.internal.R.id.content_preview_thumbnail),
                            bitmap));
        }

        displayHeadline(contentPreviewLayout, mHeadlineGenerator.getTextHeadline(mSharingText));

        return contentPreviewLayout;
    }

    private List<ActionRow.Action> createTextPreviewActions() {
        ArrayList<ActionRow.Action> actions = new ArrayList<>(2);
        actions.add(mActionFactory.createCopyButton());
        return actions;
    }
}
