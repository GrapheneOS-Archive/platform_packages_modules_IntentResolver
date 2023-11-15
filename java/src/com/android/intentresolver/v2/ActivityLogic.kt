package com.android.intentresolver.v2

import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResources.Strings.Core.FORWARD_INTENT_TO_PERSONAL
import android.app.admin.DevicePolicyResources.Strings.Core.FORWARD_INTENT_TO_WORK
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.R
import com.android.intentresolver.WorkProfileAvailabilityManager
import com.android.intentresolver.icons.TargetDataLoader

/**
 * Logic for IntentResolver Activities. Anything that is not the same across activities (including
 * test activities) should be in this interface. Expect there to be one implementation for each
 * activity, including test activities, but all implementations should delegate to a
 * CommonActivityLogic implementation.
 */
interface ActivityLogic : CommonActivityLogic {
    /** The intent for the target. This will always come before additional targets, if any. */
    val targetIntent: Intent
    /** Whether the intent is for home. */
    val resolvingHome: Boolean
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
    /** The theme to use. */
    val themeResId: Int
    /**
     * Message showing that intent is forwarded from managed profile to owner or other way around.
     */
    val profileSwitchMessage: String?
    /** The intents for potential actual targets. [targetIntent] must be first. */
    val payloadIntents: List<Intent>

    /**
     * Called after Activity superclass creation, but before any other onCreate logic is performed.
     */
    fun preInitialization()

    /** Sets [profileSwitchMessage] to null */
    fun clearProfileSwitchMessage()
}

/**
 * Logic that is common to all IntentResolver activities. Anything that is the same across
 * activities (including test activities), should live here.
 */
interface CommonActivityLogic {
    /** The tag to use when logging. */
    val tag: String
    /** A reference to the activity owning, and used by, this logic. */
    val activity: ComponentActivity
    /** The name of the referring package. */
    val referrerPackageName: String?
    /** User manager system service. */
    val userManager: UserManager
    /** Device policy manager system service. */
    val devicePolicyManager: DevicePolicyManager
    /** Current [UserHandle]s retrievable by type. */
    val annotatedUserHandles: AnnotatedUserHandles?
    /** Monitors for changes to work profile availability. */
    val workProfileAvailabilityManager: WorkProfileAvailabilityManager

    /** Returns display message indicating intent forwarding or null if not intent forwarding. */
    fun forwardMessageFor(intent: Intent): String?
}

/**
 * Concrete implementation of the [CommonActivityLogic] interface meant to be delegated to by
 * [ActivityLogic] implementations. Test implementations of [ActivityLogic] may need to create their
 * own [CommonActivityLogic] implementation.
 */
class CommonActivityLogicImpl(
    override val tag: String,
    activityProvider: () -> ComponentActivity,
    onWorkProfileStatusUpdated: () -> Unit,
) : CommonActivityLogic {

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

    override val userManager: UserManager by lazy { activity.getSystemService()!! }

    override val devicePolicyManager: DevicePolicyManager by lazy { activity.getSystemService()!! }

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        try {
            AnnotatedUserHandles.forShareActivity(activity)
        } catch (e: SecurityException) {
            Log.e(tag, "Request from UID without necessary permissions", e)
            null
        }
    }

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager by lazy {
        WorkProfileAvailabilityManager(
            userManager,
            annotatedUserHandles?.workProfileUserHandle,
            onWorkProfileStatusUpdated,
        )
    }

    private val forwardToPersonalMessage: String? by lazy {
        devicePolicyManager.resources.getString(FORWARD_INTENT_TO_PERSONAL) {
            activity.getString(R.string.forward_intent_to_owner)
        }
    }

    private val forwardToWorkMessage: String? by lazy {
        devicePolicyManager.resources.getString(FORWARD_INTENT_TO_WORK) {
            activity.getString(R.string.forward_intent_to_work)
        }
    }

    override fun forwardMessageFor(intent: Intent): String? {
        val contentUserHint = intent.contentUserHint
        if (
            contentUserHint != UserHandle.USER_CURRENT && contentUserHint != UserHandle.myUserId()
        ) {
            val originUserInfo = userManager.getUserInfo(contentUserHint)
            val originIsManaged = originUserInfo?.isManagedProfile ?: false
            val targetIsManaged = userManager.isManagedProfile
            return when {
                originIsManaged && !targetIsManaged -> forwardToPersonalMessage
                !originIsManaged && targetIsManaged -> forwardToWorkMessage
                else -> null
            }
        }
        return null
    }

    companion object {
        private const val ANDROID_APP_URI_SCHEME = "android-app"
    }
}
