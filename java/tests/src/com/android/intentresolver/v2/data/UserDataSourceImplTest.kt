@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.intentresolver.v2.data

import android.content.Intent.ACTION_PROFILE_ADDED
import android.content.Intent.ACTION_PROFILE_AVAILABLE
import android.content.Intent.ACTION_PROFILE_REMOVED
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserHandle.USER_NULL
import android.os.UserManager
import com.android.intentresolver.mock
import com.android.intentresolver.v2.coroutines.collectLastValue
import com.android.intentresolver.v2.data.User.Role
import com.android.intentresolver.v2.data.UserDataSourceImpl.UserEvent
import com.android.intentresolver.v2.platform.FakeUserManager
import com.android.intentresolver.v2.platform.FakeUserManager.ProfileType
import com.android.intentresolver.whenever
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq

internal class UserDataSourceImplTest {
    private val userManager = FakeUserManager()
    private val userState = userManager.state

    @Test
    fun initialization() = runTest {
        val dataSource = createUserDataSource(userManager)
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users)
            .containsExactly(
                userState.primaryUserHandle,
                User(userState.primaryUserHandle.identifier, Role.PERSONAL)
            )
    }

    @Test
    fun createProfile() = runTest {
        val dataSource = createUserDataSource(userManager)
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users!!.values.filter { it.role.type == User.Type.PROFILE }).isEmpty()

        val profile = userState.createProfile(ProfileType.WORK)
        assertThat(users).containsEntry(profile, User(profile.identifier, Role.WORK))
    }

    @Test
    fun removeProfile() = runTest {
        val dataSource = createUserDataSource(userManager)
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        val work = userState.createProfile(ProfileType.WORK)
        assertThat(users).containsEntry(work, User(work.identifier, Role.WORK))

        userState.removeProfile(work)
        assertThat(users).doesNotContainEntry(work, User(work.identifier, Role.WORK))
    }

    @Test
    fun isAvailable() = runTest {
        val dataSource = createUserDataSource(userManager)
        val work = userState.createProfile(ProfileType.WORK)

        val available by collectLastValue(dataSource.isAvailable(work))
        assertThat(available).isTrue()

        userState.setQuietMode(work, true)
        assertThat(available).isFalse()

        userState.setQuietMode(work, false)
        assertThat(available).isTrue()
    }

    /**
     * This and all the 'recovers_from_*' tests below all configure a static event flow instead of
     * using [FakeUserManager]. These tests verify that a invalid broadcast causes the flow to
     * reinitialize with the user profile group.
     */
    @Test
    fun recovers_from_invalid_profile_added_event() = runTest {
        val userManager =
            mockUserManager(validUser = UserHandle.USER_SYSTEM, invalidUser = USER_NULL)
        val events = flowOf(UserEvent(ACTION_PROFILE_ADDED, UserHandle.of(USER_NULL)))
        val dataSource =
            UserDataSourceImpl(
                profileParent = UserHandle.SYSTEM,
                userManager = userManager,
                userEvents = events,
                scope = backgroundScope,
                backgroundDispatcher = Dispatchers.Unconfined
            )
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users)
            .containsExactly(UserHandle.SYSTEM, User(UserHandle.USER_SYSTEM, Role.PERSONAL))
    }

    @Test
    fun recovers_from_invalid_profile_removed_event() = runTest {
        val userManager =
            mockUserManager(validUser = UserHandle.USER_SYSTEM, invalidUser = USER_NULL)
        val events = flowOf(UserEvent(ACTION_PROFILE_REMOVED, UserHandle.of(USER_NULL)))
        val dataSource =
            UserDataSourceImpl(
                profileParent = UserHandle.SYSTEM,
                userManager = userManager,
                userEvents = events,
                scope = backgroundScope,
                backgroundDispatcher = Dispatchers.Unconfined
            )
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users)
            .containsExactly(UserHandle.SYSTEM, User(UserHandle.USER_SYSTEM, Role.PERSONAL))
    }

    @Test
    fun recovers_from_invalid_profile_available_event() = runTest {
        val userManager =
            mockUserManager(validUser = UserHandle.USER_SYSTEM, invalidUser = USER_NULL)
        val events = flowOf(UserEvent(ACTION_PROFILE_AVAILABLE, UserHandle.of(USER_NULL)))
        val dataSource =
            UserDataSourceImpl(
                UserHandle.SYSTEM,
                userManager,
                events,
                backgroundScope,
                Dispatchers.Unconfined
            )
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users)
            .containsExactly(UserHandle.SYSTEM, User(UserHandle.USER_SYSTEM, Role.PERSONAL))
    }

    @Test
    fun recovers_from_unknown_event() = runTest {
        val userManager =
            mockUserManager(validUser = UserHandle.USER_SYSTEM, invalidUser = USER_NULL)
        val events = flowOf(UserEvent("UNKNOWN_EVENT", UserHandle.of(USER_NULL)))
        val dataSource =
            UserDataSourceImpl(
                profileParent = UserHandle.SYSTEM,
                userManager = userManager,
                userEvents = events,
                scope = backgroundScope,
                backgroundDispatcher = Dispatchers.Unconfined
            )
        val users by collectLastValue(dataSource.users)

        assertWithMessage("collectLast(dataSource.users)").that(users).isNotNull()
        assertThat(users)
            .containsExactly(UserHandle.SYSTEM, User(UserHandle.USER_SYSTEM, Role.PERSONAL))
    }
}

@Suppress("SameParameterValue", "DEPRECATION")
private fun mockUserManager(validUser: Int, invalidUser: Int) =
    mock<UserManager> {
        val info = UserInfo(validUser, "", "", UserInfo.FLAG_FULL)
        doReturn(listOf(info)).whenever(this).getEnabledProfiles(anyInt())

        doReturn(info).whenever(this).getUserInfo(eq(validUser))

        doReturn(listOf<UserInfo>()).whenever(this).getEnabledProfiles(eq(invalidUser))

        doReturn(null).whenever(this).getUserInfo(eq(invalidUser))
    }

private fun TestScope.createUserDataSource(userManager: FakeUserManager) =
    UserDataSourceImpl(
        profileParent = userManager.state.primaryUserHandle,
        userManager = userManager,
        userEvents = userManager.state.userEvents,
        scope = backgroundScope,
        backgroundDispatcher = Dispatchers.Unconfined
    )
