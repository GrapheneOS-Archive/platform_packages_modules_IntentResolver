/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.IntDef;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.PluralsMessageFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.intentresolver.widget.RoundedRectImageView;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Collection of helpers for building the content preview UI displayed in {@link ChooserActivity}.
 *
 * TODO: this "namespace" was pulled out of {@link ChooserActivity} as a bucket of static methods
 * to show that they're one-shot procedures with no dependencies back to {@link ChooserActivity}
 * state other than the delegates that are explicitly provided. There may be more appropriate
 * abstractions (e.g., maybe this can be a "widget" added directly to the view hierarchy to show the
 * appropriate preview), or it may at least be safe (and more convenient) to adopt a more "object
 * oriented" design where the static specifiers are removed and some of the dependencies are cached
 * as ivars when this "class" is initialized.
 */
public final class ChooserContentPreviewUi {
    /**
     * Delegate to handle background resource loads that are dependencies of content previews.
     */
    public interface ContentPreviewCoordinator {
        /**
         * Request that an image be loaded in the background and set into a view.
         *
         * @param viewProvider A delegate that will be called exactly once upon completion of the
         * load, from the UI thread, to provide the {@link RoundedRectImageView} that should be
         * populated with the result (if the load was successful) or hidden (if the load failed). If
         * this returns null, the load is discarded as a failure.
         * @param imageUri The {@link Uri} of the image to load.
         * @param extraImages The "extra image count" to set on the {@link RoundedRectImageView}
         * if the image loads successfully.
         *
         * TODO: it looks like clients are probably capable of passing the view directly, but the
         * deferred computation here is a closer match to the legacy model for now.
         */
        void loadUriIntoView(
                Callable<RoundedRectImageView> viewProvider, Uri imageUri, int extraImages);
    }

    /**
     * Delegate to build the default system action buttons to display in the preview layout, if/when
     * they're determined to be appropriate for the particular preview we display.
     * TODO: clarify why action buttons are part of preview logic.
     */
    public interface ActionButtonFactory {
        /** Create a button that copies the share content to the clipboard. */
        Button createCopyButton();

        /** Create a button that opens the share content in a system-default editor. */
        Button createEditButton();

        /** Create a "Share to Nearby" button. */
        Button createNearbyButton();
    }

    /**
     * Testing shim to specify whether a given mime type is considered to be an "image."
     *
     * TODO: move away from {@link ChooserActivityOverrideData} as a model to configure our tests,
     * then migrate {@link ChooserActivity#isImageType(String)} into this class.
     */
    public interface ImageMimeTypeClassifier {
        /** @return whether the specified {@code mimeType} is classified as an "image" type. */
        boolean isImageType(String mimeType);
    }

    @Retention(SOURCE)
    @IntDef({CONTENT_PREVIEW_FILE, CONTENT_PREVIEW_IMAGE, CONTENT_PREVIEW_TEXT})
    private @interface ContentPreviewType {
    }

    // Starting at 1 since 0 is considered "undefined" for some of the database transformations
    // of tron logs.
    @VisibleForTesting
    public static final int CONTENT_PREVIEW_IMAGE = 1;
    @VisibleForTesting
    public static final int CONTENT_PREVIEW_FILE = 2;
    @VisibleForTesting
    public static final int CONTENT_PREVIEW_TEXT = 3;

    private static final String TAG = "ChooserPreview";

    private static final String PLURALS_COUNT  = "count";
    private static final String PLURALS_FILE_NAME = "file_name";

    /** Determine the most appropriate type of preview to show for the provided {@link Intent}. */
    @ContentPreviewType
    public static int findPreferredContentPreview(
            Intent targetIntent,
            ContentResolver resolver,
            ImageMimeTypeClassifier imageClassifier) {
        /* In {@link android.content.Intent#getType}, the app may specify a very general mime type
         * that broadly covers all data being shared, such as {@literal *}/* when sending an image
         * and text. We therefore should inspect each item for the preferred type, in order: IMAGE,
         * FILE, TEXT.  */
        String action = targetIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = targetIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            return findPreferredContentPreview(uri, resolver, imageClassifier);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            List<Uri> uris = targetIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris == null || uris.isEmpty()) {
                return CONTENT_PREVIEW_TEXT;
            }

            for (Uri uri : uris) {
                // Defaulting to file preview when there are mixed image/file types is
                // preferable, as it shows the user the correct number of items being shared
                int uriPreviewType = findPreferredContentPreview(uri, resolver, imageClassifier);
                if (uriPreviewType == CONTENT_PREVIEW_FILE) {
                    return CONTENT_PREVIEW_FILE;
                }
            }

            return CONTENT_PREVIEW_IMAGE;
        }

        return CONTENT_PREVIEW_TEXT;
    }

    /**
     * Display a content preview of the specified {@code previewType} to preview the content of the
     * specified {@code intent}.
     */
    public static ViewGroup displayContentPreview(
            @ContentPreviewType int previewType,
            Intent targetIntent,
            Resources resources,
            LayoutInflater layoutInflater,
            ActionButtonFactory buttonFactory,
            ViewGroup parent,
            ContentPreviewCoordinator previewCoord,
            ContentResolver contentResolver,
            ImageMimeTypeClassifier imageClassifier) {
        ViewGroup layout = null;

        switch (previewType) {
            case CONTENT_PREVIEW_TEXT:
                layout = displayTextContentPreview(
                        targetIntent,
                        resources,
                        layoutInflater,
                        buttonFactory,
                        parent,
                        previewCoord);
                break;
            case CONTENT_PREVIEW_IMAGE:
                layout = displayImageContentPreview(
                        targetIntent,
                        resources,
                        layoutInflater,
                        buttonFactory,
                        parent,
                        previewCoord,
                        contentResolver,
                        imageClassifier);
                break;
            case CONTENT_PREVIEW_FILE:
                layout = displayFileContentPreview(
                        targetIntent,
                        resources,
                        layoutInflater,
                        buttonFactory,
                        parent,
                        previewCoord,
                        contentResolver);
                break;
            default:
                Log.e(TAG, "Unexpected content preview type: " + previewType);
        }

        return layout;
    }

    private static Cursor queryResolver(ContentResolver resolver, Uri uri) {
        return resolver.query(uri, null, null, null, null);
    }

    @ContentPreviewType
    private static int findPreferredContentPreview(
            Uri uri, ContentResolver resolver, ImageMimeTypeClassifier imageClassifier) {
        if (uri == null) {
            return CONTENT_PREVIEW_TEXT;
        }

        String mimeType = resolver.getType(uri);
        return imageClassifier.isImageType(mimeType) ? CONTENT_PREVIEW_IMAGE : CONTENT_PREVIEW_FILE;
    }

    private static ViewGroup displayTextContentPreview(
            Intent targetIntent,
            Resources resources,
            LayoutInflater layoutInflater,
            ActionButtonFactory buttonFactory,
            ViewGroup parent,
            ContentPreviewCoordinator previewCoord) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_text, parent, false);

        final ViewGroup actionRow =
                (ViewGroup) contentPreviewLayout.findViewById(
                        com.android.internal.R.id.chooser_action_row);
        final int iconMargin = resources.getDimensionPixelSize(R.dimen.resolver_icon_margin);
        addActionButton(actionRow, buttonFactory.createCopyButton(), iconMargin);
        addActionButton(actionRow, buttonFactory.createNearbyButton(), iconMargin);

        CharSequence sharingText = targetIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharingText == null) {
            contentPreviewLayout
                    .findViewById(com.android.internal.R.id.content_preview_text_layout)
                    .setVisibility(View.GONE);
        } else {
            TextView textView = contentPreviewLayout.findViewById(
                    com.android.internal.R.id.content_preview_text);
            textView.setText(sharingText);
        }

        String previewTitle = targetIntent.getStringExtra(Intent.EXTRA_TITLE);
        if (TextUtils.isEmpty(previewTitle)) {
            contentPreviewLayout
                    .findViewById(com.android.internal.R.id.content_preview_title_layout)
                    .setVisibility(View.GONE);
        } else {
            TextView previewTitleView = contentPreviewLayout.findViewById(
                    com.android.internal.R.id.content_preview_title);
            previewTitleView.setText(previewTitle);

            ClipData previewData = targetIntent.getClipData();
            Uri previewThumbnail = null;
            if (previewData != null) {
                if (previewData.getItemCount() > 0) {
                    ClipData.Item previewDataItem = previewData.getItemAt(0);
                    previewThumbnail = previewDataItem.getUri();
                }
            }

            ImageView previewThumbnailView = contentPreviewLayout.findViewById(
                    com.android.internal.R.id.content_preview_thumbnail);
            if (previewThumbnail == null) {
                previewThumbnailView.setVisibility(View.GONE);
            } else {
                previewCoord.loadUriIntoView(
                        () -> contentPreviewLayout.findViewById(
                                com.android.internal.R.id.content_preview_thumbnail),
                        previewThumbnail,
                        0);
            }
        }

        return contentPreviewLayout;
    }

    private static ViewGroup displayImageContentPreview(
            Intent targetIntent,
            Resources resources,
            LayoutInflater layoutInflater,
            ActionButtonFactory buttonFactory,
            ViewGroup parent,
            ContentPreviewCoordinator previewCoord,
            ContentResolver contentResolver,
            ImageMimeTypeClassifier imageClassifier) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_image, parent, false);
        ViewGroup imagePreview = contentPreviewLayout.findViewById(
                com.android.internal.R.id.content_preview_image_area);

        final ViewGroup actionRow =
                (ViewGroup) contentPreviewLayout.findViewById(
                        com.android.internal.R.id.chooser_action_row);
        final int iconMargin = resources.getDimensionPixelSize(R.dimen.resolver_icon_margin);
        //TODO: addActionButton(actionRow, buttonFactory.createCopyButton(), iconMargin);
        addActionButton(actionRow, buttonFactory.createNearbyButton(), iconMargin);
        addActionButton(actionRow, buttonFactory.createEditButton(), iconMargin);

        String action = targetIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = targetIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            imagePreview.findViewById(com.android.internal.R.id.content_preview_image_1_large)
                    .setTransitionName(ChooserActivity.FIRST_IMAGE_PREVIEW_TRANSITION_NAME);
            previewCoord.loadUriIntoView(
                    () -> contentPreviewLayout.findViewById(
                            com.android.internal.R.id.content_preview_image_1_large),
                    uri,
                    0);
        } else {
            List<Uri> uris = targetIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            List<Uri> imageUris = new ArrayList<>();
            for (Uri uri : uris) {
                if (imageClassifier.isImageType(contentResolver.getType(uri))) {
                    imageUris.add(uri);
                }
            }

            if (imageUris.size() == 0) {
                Log.i(TAG, "Attempted to display image preview area with zero"
                        + " available images detected in EXTRA_STREAM list");
                imagePreview.setVisibility(View.GONE);
                return contentPreviewLayout;
            }

            imagePreview.findViewById(com.android.internal.R.id.content_preview_image_1_large)
                    .setTransitionName(ChooserActivity.FIRST_IMAGE_PREVIEW_TRANSITION_NAME);
            previewCoord.loadUriIntoView(
                    () -> contentPreviewLayout.findViewById(
                            com.android.internal.R.id.content_preview_image_1_large),
                    imageUris.get(0),
                    0);

            if (imageUris.size() == 2) {
                previewCoord.loadUriIntoView(
                        () -> contentPreviewLayout.findViewById(
                                com.android.internal.R.id.content_preview_image_2_large),
                        imageUris.get(1),
                        0);
            } else if (imageUris.size() > 2) {
                previewCoord.loadUriIntoView(
                        () -> contentPreviewLayout.findViewById(
                                com.android.internal.R.id.content_preview_image_2_small),
                        imageUris.get(1),
                        0);
                previewCoord.loadUriIntoView(
                        () -> contentPreviewLayout.findViewById(
                                com.android.internal.R.id.content_preview_image_3_small),
                        imageUris.get(2),
                        imageUris.size() - 3);
            }
        }

        return contentPreviewLayout;
    }

    private static ViewGroup displayFileContentPreview(
            Intent targetIntent,
            Resources resources,
            LayoutInflater layoutInflater,
            ActionButtonFactory buttonFactory,
            ViewGroup parent,
            ContentPreviewCoordinator previewCoord,
            ContentResolver contentResolver) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_file, parent, false);

        final ViewGroup actionRow =
                (ViewGroup) contentPreviewLayout.findViewById(
                        com.android.internal.R.id.chooser_action_row);
        final int iconMargin = resources.getDimensionPixelSize(R.dimen.resolver_icon_margin);
        //TODO(b/120417119):
        // addActionButton(actionRow, buttonFactory.createCopyButton(), iconMargin);
        addActionButton(actionRow, buttonFactory.createNearbyButton(), iconMargin);

        String action = targetIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = targetIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            loadFileUriIntoView(uri, contentPreviewLayout, previewCoord, contentResolver);
        } else {
            List<Uri> uris = targetIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            int uriCount = uris.size();

            if (uriCount == 0) {
                contentPreviewLayout.setVisibility(View.GONE);
                Log.i(TAG,
                        "Appears to be no uris available in EXTRA_STREAM, removing "
                                + "preview area");
                return contentPreviewLayout;
            } else if (uriCount == 1) {
                loadFileUriIntoView(
                        uris.get(0), contentPreviewLayout, previewCoord, contentResolver);
            } else {
                FileInfo fileInfo = extractFileInfo(uris.get(0), contentResolver);
                int remUriCount = uriCount - 1;
                Map<String, Object> arguments = new HashMap<>();
                arguments.put(PLURALS_COUNT, remUriCount);
                arguments.put(PLURALS_FILE_NAME, fileInfo.name);
                String fileName =
                        PluralsMessageFormatter.format(resources, arguments, R.string.file_count);

                TextView fileNameView = contentPreviewLayout.findViewById(
                        com.android.internal.R.id.content_preview_filename);
                fileNameView.setText(fileName);

                View thumbnailView = contentPreviewLayout.findViewById(
                        com.android.internal.R.id.content_preview_file_thumbnail);
                thumbnailView.setVisibility(View.GONE);

                ImageView fileIconView = contentPreviewLayout.findViewById(
                        com.android.internal.R.id.content_preview_file_icon);
                fileIconView.setVisibility(View.VISIBLE);
                fileIconView.setImageResource(R.drawable.ic_file_copy);
            }
        }

        return contentPreviewLayout;
    }

    private static void logContentPreviewWarning(Uri uri) {
        // The ContentResolver already logs the exception. Log something more informative.
        Log.w(TAG, "Could not load (" + uri.toString() + ") thumbnail/name for preview. If "
                + "desired, consider using Intent#createChooser to launch the ChooserActivity, "
                + "and set your Intent's clipData and flags in accordance with that method's "
                + "documentation");
    }

    private static void loadFileUriIntoView(
            final Uri uri,
            final View parent,
            final ContentPreviewCoordinator previewCoord,
            final ContentResolver contentResolver) {
        FileInfo fileInfo = extractFileInfo(uri, contentResolver);

        TextView fileNameView = parent.findViewById(
                com.android.internal.R.id.content_preview_filename);
        fileNameView.setText(fileInfo.name);

        if (fileInfo.hasThumbnail) {
            previewCoord.loadUriIntoView(
                    () -> parent.findViewById(
                            com.android.internal.R.id.content_preview_file_thumbnail),
                    uri,
                    0);
        } else {
            View thumbnailView = parent.findViewById(
                    com.android.internal.R.id.content_preview_file_thumbnail);
            thumbnailView.setVisibility(View.GONE);

            ImageView fileIconView = parent.findViewById(
                    com.android.internal.R.id.content_preview_file_icon);
            fileIconView.setVisibility(View.VISIBLE);
            fileIconView.setImageResource(R.drawable.chooser_file_generic);
        }
    }

    private static void addActionButton(ViewGroup parent, Button b, int iconMargin) {
        if (b == null) {
            return;
        }
        final ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        final int gap = iconMargin / 2;
        lp.setMarginsRelative(gap, 0, gap, 0);
        parent.addView(b, lp);
    }

    private static FileInfo extractFileInfo(Uri uri, ContentResolver resolver) {
        String fileName = null;
        boolean hasThumbnail = false;

        try (Cursor cursor = queryResolver(resolver, uri)) {
            if (cursor != null && cursor.getCount() > 0) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int titleIndex = cursor.getColumnIndex(Downloads.Impl.COLUMN_TITLE);
                int flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);

                cursor.moveToFirst();
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                } else if (titleIndex != -1) {
                    fileName = cursor.getString(titleIndex);
                }

                if (flagsIndex != -1) {
                    hasThumbnail = (cursor.getInt(flagsIndex)
                            & DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL) != 0;
                }
            }
        } catch (SecurityException | NullPointerException e) {
            logContentPreviewWarning(uri);
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = uri.getPath();
            int index = fileName.lastIndexOf('/');
            if (index != -1) {
                fileName = fileName.substring(index + 1);
            }
        }

        return new FileInfo(fileName, hasThumbnail);
    }

    private static class FileInfo {
        public final String name;
        public final boolean hasThumbnail;

        FileInfo(String name, boolean hasThumbnail) {
            this.name = name;
            this.hasThumbnail = hasThumbnail;
        }
    }

    private ChooserContentPreviewUi() {}
}
