package com.android.intentresolver.v2

import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager
import com.android.intentresolver.v2.ui.model.ChooserRequest

/** Activity logic for use when testing [ChooserActivity]. */
class TestChooserActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
    chooserRequest: ChooserRequest? = null,
    private val annotatedUserHandlesOverride: AnnotatedUserHandles?,
    private val workProfileAvailabilityOverride: WorkProfileAvailabilityManager?,
) :
    ChooserActivityLogic(
        tag,
        activity,
        onWorkProfileStatusUpdated,
        chooserRequest,
    ) {
    override val annotatedUserHandles: AnnotatedUserHandles?
        get() = annotatedUserHandlesOverride ?: super.annotatedUserHandles

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager
        get() = workProfileAvailabilityOverride ?: super.workProfileAvailabilityManager
}
