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

package com.android.intentresolver.contentpreview;

import static android.provider.DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;

import static com.android.intentresolver.contentpreview.ContentPreviewType.CONTENT_PREVIEW_IMAGE;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.android.intentresolver.ImageLoader;
import com.android.intentresolver.flags.FeatureFlagRepository;
import com.android.intentresolver.flags.Flags;
import com.android.intentresolver.widget.ActionRow;
import com.android.intentresolver.widget.ImagePreviewView.TransitionElementStatusCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Collection of helpers for building the content preview UI displayed in
 * {@link com.android.intentresolver.ChooserActivity}.
 *
 * A content preview façade.
 */
public final class ChooserContentPreviewUi {
    /**
     * Delegate to build the default system action buttons to display in the preview layout, if/when
     * they're determined to be appropriate for the particular preview we display.
     * TODO: clarify why action buttons are part of preview logic.
     */
    public interface ActionFactory {
        /** Create an action that copies the share content to the clipboard. */
        ActionRow.Action createCopyButton();

        /** Create an action that opens the share content in a system-default editor. */
        @Nullable
        ActionRow.Action createEditButton();

        /** Create an "Share to Nearby" action. */
        @Nullable
        ActionRow.Action createNearbyButton();

        /** Create custom actions */
        List<ActionRow.Action> createCustomActions();

        /**
         * Provides a share modification action, if any.
         */
        @Nullable
        ActionRow.Action getModifyShareAction();

        /**
         * <p>
         * Creates an exclude-text action that can be called when the user changes shared text
         * status in the Media + Text preview.
         * </p>
         * <p>
         * <code>true</code> argument value indicates that the text should be excluded.
         * </p>
         */
        Consumer<Boolean> getExcludeSharedTextAction();
    }

    private final ContentPreviewUi mContentPreviewUi;

    public ChooserContentPreviewUi(
            Intent targetIntent,
            ContentInterface contentResolver,
            MimeTypeClassifier imageClassifier,
            ImageLoader imageLoader,
            ActionFactory actionFactory,
            TransitionElementStatusCallback transitionElementStatusCallback,
            FeatureFlagRepository featureFlagRepository) {

        mContentPreviewUi = createContentPreview(
                targetIntent,
                contentResolver,
                imageClassifier,
                imageLoader,
                actionFactory,
                transitionElementStatusCallback,
                featureFlagRepository);
        if (mContentPreviewUi.getType() != CONTENT_PREVIEW_IMAGE) {
            transitionElementStatusCallback.onAllTransitionElementsReady();
        }
    }

    private ContentPreviewUi createContentPreview(
            Intent targetIntent,
            ContentInterface contentResolver,
            MimeTypeClassifier typeClassifier,
            ImageLoader imageLoader,
            ActionFactory actionFactory,
            TransitionElementStatusCallback transitionElementStatusCallback,
            FeatureFlagRepository featureFlagRepository) {

        /* In {@link android.content.Intent#getType}, the app may specify a very general mime type
         * that broadly covers all data being shared, such as {@literal *}/* when sending an image
         * and text. We therefore should inspect each item for the preferred type, in order: IMAGE,
         * FILE, TEXT.  */
        final String action = targetIntent.getAction();
        final String type = targetIntent.getType();
        final boolean isSend = Intent.ACTION_SEND.equals(action);
        final boolean isSendMultiple = Intent.ACTION_SEND_MULTIPLE.equals(action);

        if (!(isSend || isSendMultiple)
                || (type != null && ClipDescription.compareMimeTypes(type, "text/*"))) {
            return createTextPreview(
                    targetIntent, actionFactory, imageLoader, featureFlagRepository);
        }
        List<Uri> uris = extractContentUris(targetIntent);
        if (uris.isEmpty()) {
            return createTextPreview(
                    targetIntent, actionFactory, imageLoader, featureFlagRepository);
        }
        ArrayList<FileInfo> files = new ArrayList<>(uris.size());
        int previewCount = readFileInfo(contentResolver, typeClassifier, uris, files);
        if (previewCount == 0) {
            return new FileContentPreviewUi(
                    files,
                    actionFactory,
                    imageLoader,
                    featureFlagRepository);
        }
        if (featureFlagRepository.isEnabled(Flags.SHARESHEET_SCROLLABLE_IMAGE_PREVIEW)) {
            return new UnifiedContentPreviewUi(
                    files,
                    targetIntent.getCharSequenceExtra(Intent.EXTRA_TEXT),
                    actionFactory,
                    imageLoader,
                    typeClassifier,
                    transitionElementStatusCallback,
                    featureFlagRepository);
        }
        if (previewCount < uris.size()) {
            return new FileContentPreviewUi(
                    files,
                    actionFactory,
                    imageLoader,
                    featureFlagRepository);
        }
        // The legacy (3-image) image preview is on it's way out and it's unlikely that we'd end up
        // here. To preserve the legacy behavior, before using it, check that all uris are images.
        for (FileInfo fileInfo: files) {
            if (!typeClassifier.isImageType(fileInfo.getMimeType())) {
                return new FileContentPreviewUi(
                        files,
                        actionFactory,
                        imageLoader,
                        featureFlagRepository);
            }
        }
        return new ImageContentPreviewUi(
                files.stream()
                        .map(FileInfo::getPreviewUri)
                        .filter(Objects::nonNull)
                        .toList(),
                targetIntent.getCharSequenceExtra(Intent.EXTRA_TEXT),
                actionFactory,
                imageLoader,
                transitionElementStatusCallback,
                featureFlagRepository);
    }

    public int getPreferredContentPreview() {
        return mContentPreviewUi.getType();
    }

    /**
     * Display a content preview of the specified {@code previewType} to preview the content of the
     * specified {@code intent}.
     */
    public ViewGroup displayContentPreview(
            Resources resources, LayoutInflater layoutInflater, ViewGroup parent) {

        return mContentPreviewUi.display(resources, layoutInflater, parent);
    }

    private static int readFileInfo(
            ContentInterface contentResolver,
            MimeTypeClassifier typeClassifier,
            List<Uri> uris,
            List<FileInfo> fileInfos) {
        int previewCount = 0;
        for (Uri uri: uris) {
            FileInfo fileInfo = getFileInfo(contentResolver, typeClassifier, uri);
            if (fileInfo.getPreviewUri() != null) {
                previewCount++;
            }
            fileInfos.add(fileInfo);
        }
        return previewCount;
    }

    private static FileInfo getFileInfo(
            ContentInterface resolver, MimeTypeClassifier typeClassifier, Uri uri) {
        FileInfo.Builder builder = new FileInfo.Builder(uri)
                .withName(getFileName(uri));
        String mimeType = getType(resolver, uri);
        builder.withMimeType(mimeType);
        if (typeClassifier.isImageType(mimeType)) {
            return builder.withPreviewUri(uri).build();
        }
        readFileMetadata(resolver, uri, builder);
        if (builder.getPreviewUri() == null) {
            readOtherFileTypes(resolver, uri, typeClassifier, builder);
        }
        return builder.build();
    }

    private static void readFileMetadata(
            ContentInterface resolver, Uri uri, FileInfo.Builder builder) {
        Cursor cursor = query(resolver, uri);
        if (cursor == null || !cursor.moveToFirst()) {
            return;
        }
        int flagColIdx = -1;
        int displayIconUriColIdx = -1;
        int nameColIndex = -1;
        int titleColIndex = -1;
        String[] columns = cursor.getColumnNames();
        // TODO: double-check why Cursor#getColumnInded didn't work
        for (int i = 0; i < columns.length; i++) {
            String columnName = columns[i];
            if (DocumentsContract.Document.COLUMN_FLAGS.equals(columnName)) {
                flagColIdx = i;
            } else if (MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI.equals(columnName)) {
                displayIconUriColIdx = i;
            } else if (OpenableColumns.DISPLAY_NAME.equals(columnName)) {
                nameColIndex = i;
            } else if (Downloads.Impl.COLUMN_TITLE.equals(columnName)) {
                titleColIndex = i;
            }
        }
        String fileName = "";
        if (nameColIndex >= 0) {
            fileName = cursor.getString(nameColIndex);
        } else if (titleColIndex >= 0) {
            fileName = cursor.getString(titleColIndex);
        }
        if (!TextUtils.isEmpty(fileName)) {
            builder.withName(fileName);
        }

        Uri previewUri = null;
        if (flagColIdx >= 0 && ((cursor.getInt(flagColIdx) & FLAG_SUPPORTS_THUMBNAIL) != 0)) {
            previewUri = uri;
        } else if (displayIconUriColIdx >= 0) {
            String uriStr = cursor.getString(displayIconUriColIdx);
            previewUri = uriStr == null ? null : Uri.parse(uriStr);
        }
        if (previewUri != null) {
            builder.withPreviewUri(previewUri);
        }
    }

    private static void readOtherFileTypes(
            ContentInterface resolver,
            Uri uri,
            MimeTypeClassifier typeClassifier,
            FileInfo.Builder builder) {
        String[] otherTypes = getStreamTypes(resolver, uri);
        if (otherTypes != null && otherTypes.length > 0) {
            for (String mimeType : otherTypes) {
                if (typeClassifier.isImageType(mimeType)) {
                    builder.withPreviewUri(uri);
                    break;
                }
            }
        }
    }

    private static TextContentPreviewUi createTextPreview(
            Intent targetIntent,
            ChooserContentPreviewUi.ActionFactory actionFactory,
            ImageLoader imageLoader,
            FeatureFlagRepository featureFlagRepository) {
        CharSequence sharingText = targetIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        String previewTitle = targetIntent.getStringExtra(Intent.EXTRA_TITLE);
        ClipData previewData = targetIntent.getClipData();
        Uri previewThumbnail = null;
        if (previewData != null) {
            if (previewData.getItemCount() > 0) {
                ClipData.Item previewDataItem = previewData.getItemAt(0);
                previewThumbnail = previewDataItem.getUri();
            }
        }
        return new TextContentPreviewUi(
                sharingText,
                previewTitle,
                previewThumbnail,
                actionFactory,
                imageLoader,
                featureFlagRepository);
    }

    private static List<Uri> extractContentUris(Intent targetIntent) {
        List<Uri> uris = new ArrayList<>();
        if (Intent.ACTION_SEND.equals(targetIntent.getAction())) {
            Uri uri = targetIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (ContentPreviewUi.validForContentPreview(uri)) {
                uris.add(uri);
            }
        } else {
            List<Uri> receivedUris = targetIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (receivedUris != null) {
                for (Uri uri : receivedUris) {
                    if (ContentPreviewUi.validForContentPreview(uri)) {
                        uris.add(uri);
                    }
                }
            }
        }
        return uris;
    }

    @Nullable
    private static String getType(ContentInterface resolver, Uri uri) {
        try {
            return resolver.getType(uri);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Nullable
    private static Cursor query(ContentInterface resolver, Uri uri) {
        try {
            return resolver.query(uri, null, null, null);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Nullable
    private static String[] getStreamTypes(ContentInterface resolver, Uri uri) {
        try {
            return resolver.getStreamTypes(uri, "*/*");
        } catch (RemoteException e) {
            return null;
        }
    }

    private static String getFileName(Uri uri) {
        String fileName = uri.getPath();
        fileName = fileName == null ? "" : fileName;
        int index = fileName.lastIndexOf('/');
        if (index != -1) {
            fileName = fileName.substring(index + 1);
        }
        return fileName;
    }
}
