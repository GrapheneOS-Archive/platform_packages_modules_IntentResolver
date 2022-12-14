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

package com.android.intentresolver;

import android.annotation.Nullable;
import android.content.Context;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.android.intentresolver.grid.ChooserGridAdapter;
import com.android.internal.annotations.VisibleForTesting;

/**
 * A {@link PagerAdapter} which describes the work and personal profile share sheet screens.
 */
@VisibleForTesting
public class ChooserMultiProfilePagerAdapter extends AbstractMultiProfilePagerAdapter {
    private static final int SINGLE_CELL_SPAN_SIZE = 1;

    private final ChooserProfileDescriptor[] mItems;
    private int mBottomOffset;
    private int mMaxTargetsPerRow;

    ChooserMultiProfilePagerAdapter(
            Context context,
            ChooserGridAdapter adapter,
            EmptyStateProvider emptyStateProvider,
            QuietModeManager quietModeManager,
            UserHandle workProfileUserHandle,
            int maxTargetsPerRow) {
        super(context, /* currentPage */ 0, emptyStateProvider, quietModeManager,
                workProfileUserHandle);
        mItems = new ChooserProfileDescriptor[] {
                createProfileDescriptor(adapter)
        };
        mMaxTargetsPerRow = maxTargetsPerRow;
    }

    ChooserMultiProfilePagerAdapter(
            Context context,
            ChooserGridAdapter personalAdapter,
            ChooserGridAdapter workAdapter,
            EmptyStateProvider emptyStateProvider,
            QuietModeManager quietModeManager,
            @Profile int defaultProfile,
            UserHandle workProfileUserHandle,
            int maxTargetsPerRow) {
        super(context, /* currentPage */ defaultProfile, emptyStateProvider,
                quietModeManager, workProfileUserHandle);
        mItems = new ChooserProfileDescriptor[] {
                createProfileDescriptor(personalAdapter),
                createProfileDescriptor(workAdapter)
        };
        mMaxTargetsPerRow = maxTargetsPerRow;
    }

    private ChooserProfileDescriptor createProfileDescriptor(ChooserGridAdapter adapter) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final ViewGroup rootView =
                (ViewGroup) inflater.inflate(R.layout.chooser_list_per_profile, null, false);
        ChooserProfileDescriptor profileDescriptor =
                new ChooserProfileDescriptor(rootView, adapter);
        profileDescriptor.recyclerView.setAccessibilityDelegateCompat(
                new ChooserRecyclerViewAccessibilityDelegate(profileDescriptor.recyclerView));
        return profileDescriptor;
    }

    public void setMaxTargetsPerRow(int maxTargetsPerRow) {
        mMaxTargetsPerRow = maxTargetsPerRow;
    }

    RecyclerView getListViewForIndex(int index) {
        return getItem(index).recyclerView;
    }

    @Override
    ChooserProfileDescriptor getItem(int pageIndex) {
        return mItems[pageIndex];
    }

    @Override
    int getItemCount() {
        return mItems.length;
    }

    @Override
    @VisibleForTesting
    public ChooserGridAdapter getAdapterForIndex(int pageIndex) {
        return mItems[pageIndex].chooserGridAdapter;
    }

    @Override
    @Nullable
    ChooserListAdapter getListAdapterForUserHandle(UserHandle userHandle) {
        if (getActiveListAdapter().getUserHandle().equals(userHandle)) {
            return getActiveListAdapter();
        } else if (getInactiveListAdapter() != null
                && getInactiveListAdapter().getUserHandle().equals(userHandle)) {
            return getInactiveListAdapter();
        }
        return null;
    }

    @Override
    void setupListAdapter(int pageIndex) {
        final RecyclerView recyclerView = getItem(pageIndex).recyclerView;
        ChooserGridAdapter chooserGridAdapter = getItem(pageIndex).chooserGridAdapter;
        GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
        glm.setSpanCount(mMaxTargetsPerRow);
        glm.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return chooserGridAdapter.shouldCellSpan(position)
                                ? SINGLE_CELL_SPAN_SIZE
                                : glm.getSpanCount();
                    }
                });
    }

    @Override
    @VisibleForTesting
    public ChooserListAdapter getActiveListAdapter() {
        return getAdapterForIndex(getCurrentPage()).getListAdapter();
    }

    @Override
    @VisibleForTesting
    public ChooserListAdapter getInactiveListAdapter() {
        if (getCount() == 1) {
            return null;
        }
        return getAdapterForIndex(1 - getCurrentPage()).getListAdapter();
    }

    @Override
    public ResolverListAdapter getPersonalListAdapter() {
        return getAdapterForIndex(PROFILE_PERSONAL).getListAdapter();
    }

    @Override
    @Nullable
    public ResolverListAdapter getWorkListAdapter() {
        return getAdapterForIndex(PROFILE_WORK).getListAdapter();
    }

    @Override
    ChooserGridAdapter getCurrentRootAdapter() {
        return getAdapterForIndex(getCurrentPage());
    }

    @Override
    RecyclerView getActiveAdapterView() {
        return getListViewForIndex(getCurrentPage());
    }

    @Override
    @Nullable
    RecyclerView getInactiveAdapterView() {
        if (getCount() == 1) {
            return null;
        }
        return getListViewForIndex(1 - getCurrentPage());
    }

    void setEmptyStateBottomOffset(int bottomOffset) {
        mBottomOffset = bottomOffset;
    }

    @Override
    protected void setupContainerPadding(View container) {
        int initialBottomPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.resolver_empty_state_container_padding_bottom);
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(),
                container.getPaddingRight(), initialBottomPadding + mBottomOffset);
    }

    class ChooserProfileDescriptor extends ProfileDescriptor {
        private ChooserGridAdapter chooserGridAdapter;
        private RecyclerView recyclerView;
        ChooserProfileDescriptor(ViewGroup rootView, ChooserGridAdapter adapter) {
            super(rootView);
            chooserGridAdapter = adapter;
            recyclerView = rootView.findViewById(com.android.internal.R.id.resolver_list);
        }
    }
}
