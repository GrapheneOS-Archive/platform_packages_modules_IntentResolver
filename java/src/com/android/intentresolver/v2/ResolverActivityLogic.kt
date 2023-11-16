package com.android.intentresolver.v2

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting
import com.android.intentresolver.R
import com.android.intentresolver.icons.DefaultTargetDataLoader
import com.android.intentresolver.icons.TargetDataLoader
import com.android.intentresolver.v2.util.mutableLazy

/** Activity logic for [ResolverActivity]. */
@OpenForTesting
open class ResolverActivityLogic(
    tag: String,
    activityProvider: () -> ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activityProvider,
        onWorkProfileStatusUpdated,
    ) {

    override val targetIntent: Intent by lazy {
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

    override val resolvingHome: Boolean by lazy {
        targetIntent.action == Intent.ACTION_MAIN &&
            targetIntent.categories.singleOrNull() == Intent.CATEGORY_HOME
    }

    override val title: CharSequence? = null

    override val defaultTitleResId: Int = 0

    override val initialIntents: List<Intent>? = null

    override val supportsAlwaysUseOption: Boolean = true

    override val targetDataLoader: TargetDataLoader by lazy {
        DefaultTargetDataLoader(
            activity,
            activity.lifecycle,
            activity.intent.getBooleanExtra(
                ResolverActivity.EXTRA_IS_AUDIO_CAPTURE_DEVICE,
                /* defaultValue = */ false,
            ),
        )
    }

    override val themeResId: Int = R.style.Theme_DeviceDefault_Resolver

    private val _profileSwitchMessage = mutableLazy { forwardMessageFor(targetIntent) }
    override val profileSwitchMessage: String? by _profileSwitchMessage

    override val payloadIntents: List<Intent> by lazy { listOf(targetIntent) }

    override fun preInitialization() {
        // Do nothing
    }

    override fun clearProfileSwitchMessage() {
        _profileSwitchMessage.setLazy(null)
    }
}
