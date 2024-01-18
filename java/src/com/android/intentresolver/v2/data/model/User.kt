package com.android.intentresolver.v2.data.model

import android.annotation.UserIdInt
import android.os.UserHandle
import com.android.intentresolver.v2.data.model.User.Type
import com.android.intentresolver.v2.data.model.User.Type.FULL
import com.android.intentresolver.v2.data.model.User.Type.PROFILE

/**
 * A User represents the owner of a distinct set of content.
 * * maps 1:1 to a UserHandle or UserId (Int) value.
 * * refers to either [Full][Type.FULL], or a [Profile][Type.PROFILE] user, as indicated by the
 *   [type] property.
 *
 * See
 * [Users for system developers](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/Users.md)
 *
 * ```
 * val users = listOf(
 *     User(id = 0, role = PERSONAL),
 *     User(id = 10, role = WORK),
 *     User(id = 11, role = CLONE),
 *     User(id = 12, role = PRIVATE),
 * )
 * ```
 */
data class User(
    @UserIdInt val id: Int,
    val role: Role,
) {
    val handle: UserHandle = UserHandle.of(id)

    val type: Type
        get() = role.type

    enum class Type {
        FULL,
        PROFILE
    }

    enum class Role(
        /** The type of the role user. */
        val type: Type
    ) {
        PERSONAL(FULL),
        PRIVATE(PROFILE),
        WORK(PROFILE),
        CLONE(PROFILE)
    }
}
