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
import android.util.Log;
import android.util.PluralsMessageFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.intentresolver.R;
import com.android.intentresolver.widget.ActionRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FileContentPreviewUi extends ContentPreviewUi {
    private static final String PLURALS_COUNT  = "count";
    private static final String PLURALS_FILE_NAME = "file_name";

    private final List<FileInfo> mFiles;
    private final ChooserContentPreviewUi.ActionFactory mActionFactory;
    private final HeadlineGenerator mHeadlineGenerator;

    FileContentPreviewUi(
            List<FileInfo> files,
            ChooserContentPreviewUi.ActionFactory actionFactory,
            HeadlineGenerator headlineGenerator) {
        mFiles = files;
        mActionFactory = actionFactory;
        mHeadlineGenerator = headlineGenerator;
    }

    @Override
    public int getType() {
        return ContentPreviewType.CONTENT_PREVIEW_FILE;
    }

    @Override
    public ViewGroup display(Resources resources, LayoutInflater layoutInflater, ViewGroup parent) {
        ViewGroup layout = displayInternal(resources, layoutInflater, parent);
        displayModifyShareAction(layout, mActionFactory);
        return layout;
    }

    private ViewGroup displayInternal(
            Resources resources, LayoutInflater layoutInflater, ViewGroup parent) {
        ViewGroup contentPreviewLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.chooser_grid_preview_file, parent, false);

        final int uriCount = mFiles.size();

        displayHeadline(contentPreviewLayout, mHeadlineGenerator.getFilesHeadline(mFiles.size()));

        if (uriCount == 0) {
            contentPreviewLayout.setVisibility(View.GONE);
            Log.i(TAG, "Appears to be no uris available in EXTRA_STREAM,"
                    + " removing preview area");
            return contentPreviewLayout;
        }

        FileInfo fileInfo = mFiles.get(0);
        TextView fileNameView = contentPreviewLayout.findViewById(
                R.id.content_preview_filename);
        fileNameView.setText(fileInfo.getName());

        TextView secondLine = contentPreviewLayout.findViewById(
                R.id.content_preview_more_files);
        if (uriCount > 1) {
            int remUriCount = uriCount - 1;
            Map<String, Object> arguments = new HashMap<>();
            arguments.put(PLURALS_COUNT, remUriCount);
            secondLine.setText(
                    PluralsMessageFormatter.format(resources, arguments, R.string.more_files));
        } else {
            secondLine.setVisibility(View.GONE);
        }

        final ActionRow actionRow =
                contentPreviewLayout.findViewById(com.android.internal.R.id.chooser_action_row);
        List<ActionRow.Action> actions =
                createActions(new ArrayList<>(), mActionFactory.createCustomActions());
        actionRow.setActions(actions);
        if (actions.isEmpty()) {
            contentPreviewLayout.findViewById(R.id.actions_top_divider).setVisibility(View.GONE);
        }

        return contentPreviewLayout;
    }
}
