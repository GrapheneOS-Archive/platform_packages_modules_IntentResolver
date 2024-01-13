package com.android.intentresolver.v2

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting
import com.android.intentresolver.ChooserRequestParameters

private const val TAG = "ChooserActivityLogic"

/**
 * Activity logic for [ChooserActivity].
 *
 * TODO: Make this class no longer open once [ChooserActivity] no longer needs to cast to access
 *   [chooserRequestParameters]. For now, this class being open is better than using reflection
 *   there.
 */
@OpenForTesting
open class ChooserActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activity,
        onWorkProfileStatusUpdated,
    ) {

    val chooserRequestParameters: ChooserRequestParameters? =
        try {
            ChooserRequestParameters(
                (activity as Activity).intent,
                referrerPackageName,
                (activity as Activity).referrer,
            )
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "Caller provided invalid Chooser request parameters", e)
            null
        }

    override val targetIntent: Intent = chooserRequestParameters?.targetIntent ?: Intent()

    override val resolvingHome: Boolean = false

    override val title: CharSequence? = chooserRequestParameters?.title

    override val defaultTitleResId: Int = chooserRequestParameters?.defaultTitleResource ?: 0

    override val initialIntents: List<Intent>? = chooserRequestParameters?.initialIntents?.toList()

    override val payloadIntents: List<Intent> = buildList {
        add(targetIntent)
        chooserRequestParameters?.additionalTargets?.let { addAll(it) }
    }
}
