package com.android.intentresolver.v2

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting
import com.android.intentresolver.ChooserRequestParameters
import com.android.intentresolver.R
import com.android.intentresolver.icons.TargetDataLoader
import com.android.intentresolver.v2.util.mutableLazy

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
    activityProvider: () -> ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
    targetDataLoaderProvider: () -> TargetDataLoader,
    private val onPreInitialization: () -> Unit,
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activityProvider,
        onWorkProfileStatusUpdated,
    ) {

    override val targetIntent: Intent by lazy { chooserRequestParameters?.targetIntent ?: Intent() }

    override val resolvingHome: Boolean = false

    override val title: CharSequence? by lazy { chooserRequestParameters?.title }

    override val defaultTitleResId: Int by lazy {
        chooserRequestParameters?.defaultTitleResource ?: 0
    }

    override val initialIntents: List<Intent>? by lazy {
        chooserRequestParameters?.initialIntents?.toList()
    }

    override val supportsAlwaysUseOption: Boolean = false

    override val targetDataLoader: TargetDataLoader by lazy { targetDataLoaderProvider() }

    override val themeResId: Int = R.style.Theme_DeviceDefault_Chooser

    private val _profileSwitchMessage = mutableLazy { forwardMessageFor(targetIntent) }
    override val profileSwitchMessage: String? by _profileSwitchMessage

    override val payloadIntents: List<Intent> by lazy {
        buildList {
            add(targetIntent)
            chooserRequestParameters?.additionalTargets?.let { addAll(it) }
        }
    }

    val chooserRequestParameters: ChooserRequestParameters? by lazy {
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
    }

    override fun preInitialization() {
        onPreInitialization()
    }

    override fun clearProfileSwitchMessage() {
        _profileSwitchMessage.setLazy(null)
    }
}
