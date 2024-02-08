/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.intentresolver.v2.ui.viewmodel

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.EXTRA_ALTERNATE_INTENTS
import android.content.Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
import android.content.Intent.EXTRA_CHOOSER_MODIFY_SHARE_ACTION
import android.content.Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER
import android.content.Intent.EXTRA_CHOOSER_TARGETS
import android.content.Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER
import android.content.Intent.EXTRA_EXCLUDE_COMPONENTS
import android.content.Intent.EXTRA_INITIAL_INTENTS
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_REFERRER
import android.content.Intent.EXTRA_REPLACEMENT_EXTRAS
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.IntentFilter
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.service.chooser.ChooserAction
import android.service.chooser.ChooserTarget
import android.service.chooser.Flags
import com.android.intentresolver.ChooserActivity
import com.android.intentresolver.R
import com.android.intentresolver.util.hasValidIcon
import com.android.intentresolver.v2.ext.hasAction
import com.android.intentresolver.v2.ext.ifMatch
import com.android.intentresolver.v2.ui.model.ActivityLaunch
import com.android.intentresolver.v2.ui.model.ChooserRequest
import com.android.intentresolver.v2.validation.ValidationResult
import com.android.intentresolver.v2.validation.types.IntentOrUri
import com.android.intentresolver.v2.validation.types.array
import com.android.intentresolver.v2.validation.types.value
import com.android.intentresolver.v2.validation.validateFrom

private const val MAX_CHOOSER_ACTIONS = 5
private const val MAX_INITIAL_INTENTS = 2

// TODO: replace with the new API constant, Intent#EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI
private const val EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI =
    "android.intent.extra.CHOOSER_ADDITIONAL_CONTENT_URI"
// TODO: replace with the new API constant, Intent#EXTRA_CHOOSER_FOCUSED_ITEM_POSITION
private const val EXTRA_CHOOSER_FOCUSED_ITEM_POSITION =
    "android.intent.extra.CHOOSER_FOCUSED_ITEM_POSITION"

private fun Intent.hasSendAction() = hasAction(ACTION_SEND, ACTION_SEND_MULTIPLE)

internal fun Intent.maybeAddSendActionFlags() =
    ifMatch(Intent::hasSendAction) {
        addFlags(FLAG_ACTIVITY_NEW_DOCUMENT)
        addFlags(FLAG_ACTIVITY_MULTIPLE_TASK)
    }

fun readChooserRequest(launch: ActivityLaunch): ValidationResult<ChooserRequest> {
    val extras = launch.intent.extras ?: Bundle()
    @Suppress("DEPRECATION")
    return validateFrom(extras::get) {
        val targetIntent = required(IntentOrUri(EXTRA_INTENT)).maybeAddSendActionFlags()

        val isSendAction = targetIntent.hasAction(ACTION_SEND, ACTION_SEND_MULTIPLE)

        val additionalTargets =
            optional(array<Intent>(EXTRA_ALTERNATE_INTENTS))?.map { it.maybeAddSendActionFlags() }
                ?: emptyList()

        val replacementExtras = optional(value<Bundle>(EXTRA_REPLACEMENT_EXTRAS))

        val (customTitle, defaultTitleResource) =
            if (isSendAction) {
                ignored(
                    value<CharSequence>(EXTRA_TITLE),
                    "deprecated in P. You may wish to set a preview title by using EXTRA_TITLE " +
                        "property of the wrapped EXTRA_INTENT."
                )
                null to R.string.chooseActivity
            } else {
                val custom = optional(value<CharSequence>(EXTRA_TITLE))
                custom to (custom?.let { 0 } ?: R.string.chooseActivity)
            }

        val initialIntents =
            optional(array<Intent>(EXTRA_INITIAL_INTENTS))?.take(MAX_INITIAL_INTENTS)?.map {
                it.maybeAddSendActionFlags()
            }
                ?: emptyList()

        val chosenComponentSender =
            optional(value<IntentSender>(EXTRA_CHOSEN_COMPONENT_INTENT_SENDER))

        val refinementIntentSender =
            optional(value<IntentSender>(EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER))

        val filteredComponents =
            optional(array<ComponentName>(EXTRA_EXCLUDE_COMPONENTS)) ?: emptyList()

        @Suppress("DEPRECATION")
        val callerChooserTargets =
            optional(array<ChooserTarget>(EXTRA_CHOOSER_TARGETS)) ?: emptyList()

        val retainInOnStop =
            optional(value<Boolean>(ChooserActivity.EXTRA_PRIVATE_RETAIN_IN_ON_STOP)) ?: false

        val sharedText = optional(value<CharSequence>(EXTRA_TEXT))

        val chooserActions =
            optional(array<ChooserAction>(EXTRA_CHOOSER_CUSTOM_ACTIONS))
                ?.filter { hasValidIcon(it) }
                ?.take(MAX_CHOOSER_ACTIONS)
                ?: emptyList()

        val modifyShareAction = optional(value<ChooserAction>(EXTRA_CHOOSER_MODIFY_SHARE_ACTION))

        val referrerFillIn = Intent().putExtra(EXTRA_REFERRER, launch.referrer)

        val additionalContentUri: Uri?
        val focusedItemPos: Int
        if (isSendAction && Flags.chooserPayloadToggling()) {
            additionalContentUri = optional(value<Uri>(EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI))
            focusedItemPos = optional(value<Int>(EXTRA_CHOOSER_FOCUSED_ITEM_POSITION)) ?: 0
        } else {
            additionalContentUri = null
            focusedItemPos = 0
        }

        ChooserRequest(
            targetIntent = targetIntent,
            targetAction = targetIntent.action,
            isSendActionTarget = isSendAction,
            targetType = targetIntent.type,
            launchedFromPackage =
                requireNotNull(launch.fromPackage) {
                    "launch.fromPackage was null, See Activity.getLaunchedFromPackage()"
                },
            title = customTitle,
            defaultTitleResource = defaultTitleResource,
            referrer = launch.referrer,
            filteredComponentNames = filteredComponents,
            callerChooserTargets = callerChooserTargets,
            chooserActions = chooserActions,
            modifyShareAction = modifyShareAction,
            shouldRetainInOnStop = retainInOnStop,
            additionalTargets = additionalTargets,
            replacementExtras = replacementExtras,
            initialIntents = initialIntents,
            chosenComponentSender = chosenComponentSender,
            refinementIntentSender = refinementIntentSender,
            sharedText = sharedText,
            shareTargetFilter = targetIntent.toShareTargetFilter(),
            additionalContentUri = additionalContentUri,
            focusedItemPosition = focusedItemPos,
        )
    }
}

private fun Intent.toShareTargetFilter(): IntentFilter? {
    return type?.let {
        IntentFilter().apply {
            action?.also { addAction(it) }
            addDataType(it)
        }
    }
}
