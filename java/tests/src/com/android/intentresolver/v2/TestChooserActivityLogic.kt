package com.android.intentresolver.v2

import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.icons.TargetDataLoader

/** Activity logic for use when testing [ChooserActivity]. */
class TestChooserActivityLogic(
    tag: String,
    activityProvider: () -> ComponentActivity,
    targetDataLoaderProvider: () -> TargetDataLoader,
    onPreinitialization: () -> Unit,
    overrideData: ChooserActivityOverrideData,
) :
    ChooserActivityLogic(
        tag,
        activityProvider,
        targetDataLoaderProvider,
        onPreinitialization,
    ) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }
}
