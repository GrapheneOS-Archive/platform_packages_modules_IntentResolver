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
package com.android.intentresolver.v2.ui.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class CallerInfo(
    val launchedFromUid: Int,
    val launchedFomPackage: String?,
    /* logged to metrics, forwarded to outgoing intent */
    val referrer: Uri
) : Parcelable {
    constructor(
        source: Parcel
    ) : this(
        launchedFromUid = source.readInt(),
        launchedFomPackage = source.readString(),
        checkNotNull(source.readParcelable())
    )

    override fun describeContents() = 0 /* flags */

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(launchedFromUid)
        dest.writeString(launchedFomPackage)
        dest.writeParcelable(referrer, 0)
    }

    companion object {
        const val SAVED_STATE_HANDLE_KEY = "com.android.intentresolver.CALLER_INFO"

        @JvmStatic
        @Suppress("unused")
        val CREATOR =
            object : Parcelable.Creator<CallerInfo> {
                override fun newArray(size: Int) = arrayOfNulls<CallerInfo>(size)
                override fun createFromParcel(source: Parcel) = CallerInfo(source)
            }
    }
}

inline fun <reified T> Parcel.readParcelable(): T? {
    return readParcelable(T::class.java.classLoader, T::class.java)
}
