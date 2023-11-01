package com.android.intentresolver.v2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.android.intentresolver.icons.TargetDataLoader

/**
 * Logic for IntentResolver Activities. Anything that is not the same across activities (including
 * test activities) should be in this interface. Expect there to be one implementation for each
 * activity, including test activities, but all implementations should delegate to a
 * CommonActivityLogic implementation.
 */
interface ActivityLogic : CommonActivityLogic {
    /** The intent for the target. This will always come before [additionalTargets], if any. */
    val targetIntent: Intent
    /** Whether the intent is for home. */
    val resolvingHome: Boolean
    /** Intents for additional targets. These will always come after [targetIntent]. */
    val additionalTargets: List<Intent>?
    /** Custom title to display. */
    val title: CharSequence?
    /** Resource ID for the title to display when there is no custom title. */
    val defaultTitleResId: Int
    /** Intents received to be processed. */
    val initialIntents: List<Intent>?
    /** Whether or not this activity supports choosing a default handler for the intent. */
    val supportsAlwaysUseOption: Boolean
    /** Fetches display info for processed candidates. */
    val targetDataLoader: TargetDataLoader

    /**
     * Called after Activity superclass creation, but before any other onCreate logic is performed.
     */
    fun preInitialization()
}

/**
 * Logic that is common to all IntentResolver activities. Anything that is the same across
 * activities (including test activities), should live here.
 */
interface CommonActivityLogic {
    /** A reference to the activity owning, and used by, this logic. */
    val activity: ComponentActivity
    /** The name of the referring package. */
    val referrerPackageName: String?

    // TODO: For some reason the IDE complains about getting Activity fields from a
    //  ComponentActivity. These are a band-aid until the bug is fixed and should be removed when
    //  possible.
    val ComponentActivity.context: Context
    val ComponentActivity.intent: Intent
    val ComponentActivity.referrer: Uri?
}

/**
 * Concrete implementation of the [CommonActivityLogic] interface meant to be delegated to by
 * [ActivityLogic] implementations. Test implementations of [ActivityLogic] may need to create their
 * own [CommonActivityLogic] implementation.
 */
class CommonActivityLogicImpl(activityProvider: () -> ComponentActivity) : CommonActivityLogic {

    override val activity: ComponentActivity by lazy { activityProvider() }

    override val referrerPackageName: String? by lazy {
        activity.referrer.let {
            if (ANDROID_APP_URI_SCHEME == it?.scheme) {
                it.host
            } else {
                null
            }
        }
    }

    companion object {
        private const val ANDROID_APP_URI_SCHEME = "android-app"
    }

    // TODO: For some reason the IDE complains about getting Activity fields from a
    //  ComponentActivity. These are a band-aid until the bug is fixed and should be removed when
    //  possible.
    override val ComponentActivity.context: Context
        get() = (this as Activity)
    override val ComponentActivity.intent: Intent
        get() = (this as Activity).intent
    override val ComponentActivity.referrer: Uri?
        get() = (this as Activity).referrer
}
