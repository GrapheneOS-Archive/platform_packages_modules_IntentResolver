package com.android.intentresolver.v2

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting

/** Activity logic for [ResolverActivity]. */
@OpenForTesting
open class ResolverActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activity,
        onWorkProfileStatusUpdated,
    ) {

    final override val targetIntent: Intent = let {
        val intent = Intent(activity.intent)
        intent.setComponent(null)
        // The resolver activity is set to be hidden from recent tasks.
        // we don't want this attribute to be propagated to the next activity
        // being launched. Note that if the original Intent also had this
        // flag set, we are now losing it. That should be a very rare case
        // and we can live with this.
        intent.setFlags(intent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS.inv())

        // If FLAG_ACTIVITY_LAUNCH_ADJACENT was set, ResolverActivity was opened in the alternate
        // side, which means we want to open the target app on the same side as ResolverActivity.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT != 0) {
            intent.setFlags(intent.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT.inv())
        }
        intent
    }

    override val title: CharSequence? = null

    override val defaultTitleResId: Int = 0

    override val initialIntents: List<Intent>? = null

    override val payloadIntents: List<Intent> = listOf(targetIntent)
}
