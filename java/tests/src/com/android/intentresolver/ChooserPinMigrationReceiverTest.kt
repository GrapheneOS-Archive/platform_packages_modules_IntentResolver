/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class ChooserPinMigrationReceiverTest {

    private lateinit var chooserPinMigrationReceiver: ChooserPinMigrationReceiver

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockSharedPreferences: SharedPreferences
    @Mock private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        chooserPinMigrationReceiver = ChooserPinMigrationReceiver { mockSharedPreferences }
    }

    @Test
    fun onReceive_addsReceivedPins() {
        // Arrange
        val intent = Intent().apply {
            putExtra("TestPackage/TestClass", true)
        }

        // Act
        chooserPinMigrationReceiver.onReceive(mockContext, intent)

        // Assert
        verify(mockSharedPreferencesEditor).putBoolean(eq("TestPackage/TestClass"), eq(true))
        verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun onReceive_ignoresUnpinnedEntries() {
        // Arrange
        val intent = Intent().apply {
            putExtra("TestPackage/TestClass", false)
        }

        // Act
        chooserPinMigrationReceiver.onReceive(mockContext, intent)

        // Assert
        verify(mockSharedPreferencesEditor).apply()
        verifyNoMoreInteractions(mockSharedPreferencesEditor)
    }
}