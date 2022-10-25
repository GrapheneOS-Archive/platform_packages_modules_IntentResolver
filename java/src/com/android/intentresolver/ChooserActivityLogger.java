/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Intent;
import android.provider.MediaStore;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.InstanceId;
import com.android.internal.logging.InstanceIdSequence;
import com.android.internal.logging.UiEvent;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.util.FrameworkStatsLog;

/**
 * Helper for writing Sharesheet atoms to statsd log.
 * @hide
 */
public class ChooserActivityLogger {
    /**
     * This shim is provided only for testing. In production, clients will only ever use a
     * {@link DefaultFrameworkStatsLogger}.
     */
    @VisibleForTesting
    interface FrameworkStatsLogger {
        /** Overload to use for logging {@code FrameworkStatsLog.SHARESHEET_STARTED}. */
        void write(
                int frameworkEventId,
                int appEventId,
                String packageName,
                int instanceId,
                String mimeType,
                int numAppProvidedDirectTargets,
                int numAppProvidedAppTargets,
                boolean isWorkProfile,
                int previewType,
                int intentType);

        /** Overload to use for logging {@code FrameworkStatsLog.RANKING_SELECTED}. */
        void write(
                int frameworkEventId,
                int appEventId,
                String packageName,
                int instanceId,
                int positionPicked,
                boolean isPinned);
    }

    private static final int SHARESHEET_INSTANCE_ID_MAX = (1 << 13);

    // A small per-notification ID, used for statsd logging.
    // TODO: consider precomputing and storing as final.
    private static InstanceIdSequence sInstanceIdSequence;
    private InstanceId mInstanceId;

    private final UiEventLogger mUiEventLogger;
    private final FrameworkStatsLogger mFrameworkStatsLogger;

    public ChooserActivityLogger() {
        this(new UiEventLoggerImpl(), new DefaultFrameworkStatsLogger());
    }

    @VisibleForTesting
    ChooserActivityLogger(UiEventLogger uiEventLogger, FrameworkStatsLogger frameworkLogger) {
        mUiEventLogger = uiEventLogger;
        mFrameworkStatsLogger = frameworkLogger;
    }

    /** Logs a UiEventReported event for the system sharesheet completing initial start-up. */
    public void logShareStarted(int eventId, String packageName, String mimeType,
            int appProvidedDirect, int appProvidedApp, boolean isWorkprofile, int previewType,
            String intent) {
        mFrameworkStatsLogger.write(FrameworkStatsLog.SHARESHEET_STARTED,
                /* event_id = 1 */ SharesheetStartedEvent.SHARE_STARTED.getId(),
                /* package_name = 2 */ packageName,
                /* instance_id = 3 */ getInstanceId().getId(),
                /* mime_type = 4 */ mimeType,
                /* num_app_provided_direct_targets = 5 */ appProvidedDirect,
                /* num_app_provided_app_targets = 6 */ appProvidedApp,
                /* is_workprofile = 7 */ isWorkprofile,
                /* previewType = 8 */ typeFromPreviewInt(previewType),
                /* intentType = 9 */ typeFromIntentString(intent));
    }

    /** Logs a UiEventReported event for the system sharesheet when the user selects a target. */
    public void logShareTargetSelected(int targetType, String packageName, int positionPicked,
            boolean isPinned) {
        mFrameworkStatsLogger.write(FrameworkStatsLog.RANKING_SELECTED,
                /* event_id = 1 */ SharesheetTargetSelectedEvent.fromTargetType(targetType).getId(),
                /* package_name = 2 */ packageName,
                /* instance_id = 3 */ getInstanceId().getId(),
                /* position_picked = 4 */ positionPicked,
                /* is_pinned = 5 */ isPinned);
    }

    /** Logs a UiEventReported event for the system sharesheet being triggered by the user. */
    public void logSharesheetTriggered() {
        log(SharesheetStandardEvent.SHARESHEET_TRIGGERED, getInstanceId());
    }

    /** Logs a UiEventReported event for the system sharesheet completing loading app targets. */
    public void logSharesheetAppLoadComplete() {
        log(SharesheetStandardEvent.SHARESHEET_APP_LOAD_COMPLETE, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for the system sharesheet completing loading service targets.
     */
    public void logSharesheetDirectLoadComplete() {
        log(SharesheetStandardEvent.SHARESHEET_DIRECT_LOAD_COMPLETE, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for the system sharesheet timing out loading service targets.
     */
    public void logSharesheetDirectLoadTimeout() {
        log(SharesheetStandardEvent.SHARESHEET_DIRECT_LOAD_TIMEOUT, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for the system sharesheet switching
     * between work and main profile.
     */
    public void logSharesheetProfileChanged() {
        log(SharesheetStandardEvent.SHARESHEET_PROFILE_CHANGED, getInstanceId());
    }

    /** Logs a UiEventReported event for the system sharesheet getting expanded or collapsed. */
    public void logSharesheetExpansionChanged(boolean isCollapsed) {
        log(isCollapsed ? SharesheetStandardEvent.SHARESHEET_COLLAPSED :
                SharesheetStandardEvent.SHARESHEET_EXPANDED, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for the system sharesheet app share ranking timing out.
     */
    public void logSharesheetAppShareRankingTimeout() {
        log(SharesheetStandardEvent.SHARESHEET_APP_SHARE_RANKING_TIMEOUT, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for the system sharesheet when direct share row is empty.
     */
    public void logSharesheetEmptyDirectShareRow() {
        log(SharesheetStandardEvent.SHARESHEET_EMPTY_DIRECT_SHARE_ROW, getInstanceId());
    }

    /**
     * Logs a UiEventReported event for a given share activity
     * @param event
     * @param instanceId
     */
    private void log(UiEventLogger.UiEventEnum event, InstanceId instanceId) {
        mUiEventLogger.logWithInstanceId(
                event,
                0,
                null,
                instanceId);
    }

    /**
     * @return A unique {@link InstanceId} to join across events recorded by this logger instance.
     */
    private InstanceId getInstanceId() {
        if (mInstanceId == null) {
            if (sInstanceIdSequence == null) {
                sInstanceIdSequence = new InstanceIdSequence(SHARESHEET_INSTANCE_ID_MAX);
            }
            mInstanceId = sInstanceIdSequence.newInstanceId();
        }
        return mInstanceId;
    }

    /**
     * The UiEvent enums that this class can log.
     */
    enum SharesheetStartedEvent implements UiEventLogger.UiEventEnum {
        @UiEvent(doc = "Basic system Sharesheet has started and is visible.")
        SHARE_STARTED(228);

        private final int mId;
        SharesheetStartedEvent(int id) {
            mId = id;
        }
        @Override
        public int getId() {
            return mId;
        }
    }

    /**
     * The UiEvent enums that this class can log.
     */
    enum SharesheetTargetSelectedEvent implements UiEventLogger.UiEventEnum {
        INVALID(0),
        @UiEvent(doc = "User selected a service target.")
        SHARESHEET_SERVICE_TARGET_SELECTED(232),
        @UiEvent(doc = "User selected an app target.")
        SHARESHEET_APP_TARGET_SELECTED(233),
        @UiEvent(doc = "User selected a standard target.")
        SHARESHEET_STANDARD_TARGET_SELECTED(234),
        @UiEvent(doc = "User selected the copy target.")
        SHARESHEET_COPY_TARGET_SELECTED(235),
        @UiEvent(doc = "User selected the nearby target.")
        SHARESHEET_NEARBY_TARGET_SELECTED(626),
        @UiEvent(doc = "User selected the edit target.")
        SHARESHEET_EDIT_TARGET_SELECTED(669);

        private final int mId;
        SharesheetTargetSelectedEvent(int id) {
            mId = id;
        }
        @Override public int getId() {
            return mId;
        }

        public static SharesheetTargetSelectedEvent fromTargetType(int targetType) {
            switch(targetType) {
                case ChooserActivity.SELECTION_TYPE_SERVICE:
                    return SHARESHEET_SERVICE_TARGET_SELECTED;
                case ChooserActivity.SELECTION_TYPE_APP:
                    return SHARESHEET_APP_TARGET_SELECTED;
                case ChooserActivity.SELECTION_TYPE_STANDARD:
                    return SHARESHEET_STANDARD_TARGET_SELECTED;
                case ChooserActivity.SELECTION_TYPE_COPY:
                    return SHARESHEET_COPY_TARGET_SELECTED;
                case ChooserActivity.SELECTION_TYPE_NEARBY:
                    return SHARESHEET_NEARBY_TARGET_SELECTED;
                case ChooserActivity.SELECTION_TYPE_EDIT:
                    return SHARESHEET_EDIT_TARGET_SELECTED;
                default:
                    return INVALID;
            }
        }
    }

    /**
     * The UiEvent enums that this class can log.
     */
    enum SharesheetStandardEvent implements UiEventLogger.UiEventEnum {
        INVALID(0),
        @UiEvent(doc = "User clicked share.")
        SHARESHEET_TRIGGERED(227),
        @UiEvent(doc = "User changed from work to personal profile or vice versa.")
        SHARESHEET_PROFILE_CHANGED(229),
        @UiEvent(doc = "User expanded target list.")
        SHARESHEET_EXPANDED(230),
        @UiEvent(doc = "User collapsed target list.")
        SHARESHEET_COLLAPSED(231),
        @UiEvent(doc = "Sharesheet app targets is fully populated.")
        SHARESHEET_APP_LOAD_COMPLETE(322),
        @UiEvent(doc = "Sharesheet direct targets is fully populated.")
        SHARESHEET_DIRECT_LOAD_COMPLETE(323),
        @UiEvent(doc = "Sharesheet direct targets timed out.")
        SHARESHEET_DIRECT_LOAD_TIMEOUT(324),
        @UiEvent(doc = "Sharesheet app share ranking timed out.")
        SHARESHEET_APP_SHARE_RANKING_TIMEOUT(831),
        @UiEvent(doc = "Sharesheet empty direct share row.")
        SHARESHEET_EMPTY_DIRECT_SHARE_ROW(828);

        private final int mId;
        SharesheetStandardEvent(int id) {
            mId = id;
        }
        @Override public int getId() {
            return mId;
        }
    }

    /**
     * Returns the enum used in sharesheet started atom to indicate what preview type was used.
     */
    private static int typeFromPreviewInt(int previewType) {
        switch(previewType) {
            case ChooserActivity.CONTENT_PREVIEW_IMAGE:
                return FrameworkStatsLog.SHARESHEET_STARTED__PREVIEW_TYPE__CONTENT_PREVIEW_IMAGE;
            case ChooserActivity.CONTENT_PREVIEW_FILE:
                return FrameworkStatsLog.SHARESHEET_STARTED__PREVIEW_TYPE__CONTENT_PREVIEW_FILE;
            case ChooserActivity.CONTENT_PREVIEW_TEXT:
            default:
                return FrameworkStatsLog
                        .SHARESHEET_STARTED__PREVIEW_TYPE__CONTENT_PREVIEW_TYPE_UNKNOWN;
        }
    }

    /**
     * Returns the enum used in sharesheet started atom to indicate what intent triggers the
     * ChooserActivity.
     */
    private static int typeFromIntentString(String intent) {
        if (intent == null) {
            return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_DEFAULT;
        }
        switch (intent) {
            case Intent.ACTION_VIEW:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_VIEW;
            case Intent.ACTION_EDIT:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_EDIT;
            case Intent.ACTION_SEND:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_SEND;
            case Intent.ACTION_SENDTO:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_SENDTO;
            case Intent.ACTION_SEND_MULTIPLE:
                return FrameworkStatsLog
                        .SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_SEND_MULTIPLE;
            case MediaStore.ACTION_IMAGE_CAPTURE:
                return FrameworkStatsLog
                        .SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_IMAGE_CAPTURE;
            case Intent.ACTION_MAIN:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_MAIN;
            default:
                return FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_DEFAULT;
        }
    }

    private static class DefaultFrameworkStatsLogger implements FrameworkStatsLogger {
        @Override
        public void write(
                int frameworkEventId,
                int appEventId,
                String packageName,
                int instanceId,
                String mimeType,
                int numAppProvidedDirectTargets,
                int numAppProvidedAppTargets,
                boolean isWorkProfile,
                int previewType,
                int intentType) {
            FrameworkStatsLog.write(
                    frameworkEventId,
                    /* event_id = 1 */ appEventId,
                    /* package_name = 2 */ packageName,
                    /* instance_id = 3 */ instanceId,
                    /* mime_type = 4 */ mimeType,
                    /* num_app_provided_direct_targets */ numAppProvidedDirectTargets,
                    /* num_app_provided_app_targets */ numAppProvidedAppTargets,
                    /* is_workprofile */ isWorkProfile,
                    /* previewType = 8 */ previewType,
                    /* intentType = 9 */ intentType);
        }

        @Override
        public void write(
                int frameworkEventId,
                int appEventId,
                String packageName,
                int instanceId,
                int positionPicked,
                boolean isPinned) {
            FrameworkStatsLog.write(
                    frameworkEventId,
                    /* event_id = 1 */ appEventId,
                    /* package_name = 2 */ packageName,
                    /* instance_id = 3 */ instanceId,
                    /* position_picked = 4 */ positionPicked,
                    /* is_pinned = 5 */ isPinned);
        }
    }
}
