package com.android.intentresolver.v2

import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles

/** Activity logic for use when testing [ResolverActivity]. */
class TestResolverActivityLogic(
    tag: String,
    activityProvider: () -> ComponentActivity,
    overrideData: ResolverWrapperActivity.OverrideData,
) : ActivityLogic by ResolverActivityLogic(tag, activityProvider) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }
}
