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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.Intent;

import com.android.intentresolver.ChooserActivityLogger.FrameworkStatsLogger;
import com.android.intentresolver.ChooserActivityLogger.SharesheetStandardEvent;
import com.android.intentresolver.ChooserActivityLogger.SharesheetStartedEvent;
import com.android.intentresolver.ChooserActivityLogger.SharesheetTargetSelectedEvent;
import com.android.internal.logging.InstanceId;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLogger.UiEventEnum;
import com.android.internal.util.FrameworkStatsLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class ChooserActivityLoggerTest {
    @Mock private UiEventLogger mUiEventLog;
    @Mock private FrameworkStatsLogger mFrameworkLog;

    private ChooserActivityLogger mChooserLogger;

    @Before
    public void setUp() {
        mChooserLogger = new ChooserActivityLogger(mUiEventLog, mFrameworkLog);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mUiEventLog);
        verifyNoMoreInteractions(mFrameworkLog);
    }

    @Test
    public void testLogShareStarted() {
        final int eventId = -1;  // Passed-in eventId is unused. TODO: remove from method signature.
        final String packageName = "com.test.foo";
        final String mimeType = "text/plain";
        final int appProvidedDirectTargets = 123;
        final int appProvidedAppTargets = 456;
        final boolean workProfile = true;
        final int previewType = ChooserActivity.CONTENT_PREVIEW_FILE;
        final String intentAction = Intent.ACTION_SENDTO;

        mChooserLogger.logShareStarted(
                eventId,
                packageName,
                mimeType,
                appProvidedDirectTargets,
                appProvidedAppTargets,
                workProfile,
                previewType,
                intentAction);

        verify(mFrameworkLog).write(
                eq(FrameworkStatsLog.SHARESHEET_STARTED),
                eq(SharesheetStartedEvent.SHARE_STARTED.getId()),
                eq(packageName),
                /* instanceId=*/ gt(0),
                eq(mimeType),
                eq(appProvidedDirectTargets),
                eq(appProvidedAppTargets),
                eq(workProfile),
                eq(FrameworkStatsLog.SHARESHEET_STARTED__PREVIEW_TYPE__CONTENT_PREVIEW_FILE),
                eq(FrameworkStatsLog.SHARESHEET_STARTED__INTENT_TYPE__INTENT_ACTION_SENDTO));
    }

    @Test
    public void testLogShareTargetSelected() {
        final int targetType = ChooserActivity.SELECTION_TYPE_COPY;
        final String packageName = "com.test.foo";
        final int positionPicked = 123;
        final boolean pinned = true;

        mChooserLogger.logShareTargetSelected(targetType, packageName, positionPicked, pinned);

        verify(mFrameworkLog).write(
                eq(FrameworkStatsLog.RANKING_SELECTED),
                eq(SharesheetTargetSelectedEvent.SHARESHEET_COPY_TARGET_SELECTED.getId()),
                eq(packageName),
                /* instanceId=*/ gt(0),
                eq(positionPicked),
                eq(pinned));
    }

    @Test
    public void testLogSharesheetTriggered() {
        mChooserLogger.logSharesheetTriggered();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_TRIGGERED), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetAppLoadComplete() {
        mChooserLogger.logSharesheetAppLoadComplete();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_APP_LOAD_COMPLETE), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetDirectLoadComplete() {
        mChooserLogger.logSharesheetDirectLoadComplete();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_DIRECT_LOAD_COMPLETE),
                eq(0),
                isNull(),
                any());
    }

    @Test
    public void testLogSharesheetDirectLoadTimeout() {
        mChooserLogger.logSharesheetDirectLoadTimeout();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_DIRECT_LOAD_TIMEOUT), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetProfileChanged() {
        mChooserLogger.logSharesheetProfileChanged();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_PROFILE_CHANGED), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetExpansionChanged_collapsed() {
        mChooserLogger.logSharesheetExpansionChanged(/* isCollapsed=*/ true);
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_COLLAPSED), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetExpansionChanged_expanded() {
        mChooserLogger.logSharesheetExpansionChanged(/* isCollapsed=*/ false);
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_EXPANDED), eq(0), isNull(), any());
    }

    @Test
    public void testLogSharesheetAppShareRankingTimeout() {
        mChooserLogger.logSharesheetAppShareRankingTimeout();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_APP_SHARE_RANKING_TIMEOUT),
                eq(0),
                isNull(),
                any());
    }

    @Test
    public void testLogSharesheetEmptyDirectShareRow() {
        mChooserLogger.logSharesheetEmptyDirectShareRow();
        verify(mUiEventLog).logWithInstanceId(
                eq(SharesheetStandardEvent.SHARESHEET_EMPTY_DIRECT_SHARE_ROW),
                eq(0),
                isNull(),
                any());
    }

    @Test
    public void testDifferentLoggerInstancesUseDifferentInstanceIds() {
        ArgumentCaptor<Integer> idIntCaptor = ArgumentCaptor.forClass(Integer.class);
        ChooserActivityLogger chooserLogger2 =
                new ChooserActivityLogger(mUiEventLog, mFrameworkLog);

        final int targetType = ChooserActivity.SELECTION_TYPE_COPY;
        final String packageName = "com.test.foo";
        final int positionPicked = 123;
        final boolean pinned = true;

        mChooserLogger.logShareTargetSelected(targetType, packageName, positionPicked, pinned);
        chooserLogger2.logShareTargetSelected(targetType, packageName, positionPicked, pinned);

        verify(mFrameworkLog, times(2)).write(
                anyInt(), anyInt(), anyString(), idIntCaptor.capture(), anyInt(), anyBoolean());

        int id1 = idIntCaptor.getAllValues().get(0);
        int id2 = idIntCaptor.getAllValues().get(1);

        assertThat(id1).isGreaterThan(0);
        assertThat(id2).isGreaterThan(0);
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    public void testUiAndFrameworkEventsUseSameInstanceIdForSameLoggerInstance() {
        ArgumentCaptor<Integer> idIntCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<InstanceId> idObjectCaptor = ArgumentCaptor.forClass(InstanceId.class);

        final int targetType = ChooserActivity.SELECTION_TYPE_COPY;
        final String packageName = "com.test.foo";
        final int positionPicked = 123;
        final boolean pinned = true;

        mChooserLogger.logShareTargetSelected(targetType, packageName, positionPicked, pinned);
        verify(mFrameworkLog).write(
                anyInt(), anyInt(), anyString(), idIntCaptor.capture(), anyInt(), anyBoolean());

        mChooserLogger.logSharesheetTriggered();
        verify(mUiEventLog).logWithInstanceId(
                any(UiEventEnum.class), anyInt(), any(), idObjectCaptor.capture());

        assertThat(idIntCaptor.getValue()).isGreaterThan(0);
        assertThat(idObjectCaptor.getValue().getId()).isEqualTo(idIntCaptor.getValue());
    }
}
