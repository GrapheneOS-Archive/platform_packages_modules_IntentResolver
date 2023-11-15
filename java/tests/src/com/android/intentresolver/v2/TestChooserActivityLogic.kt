package com.android.intentresolver.v2

import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager
import com.android.intentresolver.icons.TargetDataLoader

/** Activity logic for use when testing [ChooserActivity]. */
class TestChooserActivityLogic(
    tag: String,
    activityProvider: () -> ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
    targetDataLoaderProvider: () -> TargetDataLoader,
    onPreinitialization: () -> Unit,
    private val overrideData: ChooserActivityOverrideData,
) :
    ChooserActivityLogic(
        tag,
        activityProvider,
        onWorkProfileStatusUpdated,
        targetDataLoaderProvider,
        onPreinitialization,
    ) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager by lazy {
        overrideData.mWorkProfileAvailability ?: super.workProfileAvailabilityManager
    }
}
