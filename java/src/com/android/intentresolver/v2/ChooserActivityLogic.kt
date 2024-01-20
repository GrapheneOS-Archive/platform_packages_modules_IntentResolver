package com.android.intentresolver.v2

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting
import com.android.intentresolver.v2.ui.model.ChooserRequest

private const val TAG = "ChooserActivityLogic"

/**
 * Activity logic for [ChooserActivity].
 *
 * TODO: Make this class no longer open once [ChooserActivity] no longer needs to cast to access
 *   [chooserRequest]. For now, this class being open is better than using reflection there.
 */
@OpenForTesting
open class ChooserActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
    private val chooserRequest: ChooserRequest? = null,
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activity,
        onWorkProfileStatusUpdated,
    ) {

    override val targetIntent: Intent = chooserRequest?.targetIntent ?: Intent()

    override val title: CharSequence? = chooserRequest?.title

    override val defaultTitleResId: Int = chooserRequest?.defaultTitleResource ?: 0

    override val initialIntents: List<Intent>? = chooserRequest?.initialIntents?.toList()

    override val payloadIntents: List<Intent> = buildList {
        add(targetIntent)
        chooserRequest?.additionalTargets?.let { addAll(it) }
    }
}
