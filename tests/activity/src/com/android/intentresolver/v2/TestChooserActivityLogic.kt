package com.android.intentresolver.v2

import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager
import com.android.intentresolver.icons.TargetDataLoader

/** Activity logic for use when testing [ChooserActivity]. */
class TestChooserActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
    targetDataLoader: TargetDataLoader,
    private val overrideData: ChooserActivityOverrideData,
) :
    ChooserActivityLogic(
        tag,
        activity,
        onWorkProfileStatusUpdated,
        targetDataLoader,
    ) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager by lazy {
        overrideData.mWorkProfileAvailability ?: super.workProfileAvailabilityManager
    }
}
