/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.intentresolver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.internal.app.ChooserActivity;
import com.android.internal.app.ChooserActivityOverrideData;
import com.android.internal.app.ChooserActivityTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

@RunWith(Parameterized.class)
public class UnbundledChooserActivityTest extends ChooserActivityTest {
    private static final Function<PackageManager, PackageManager> DEFAULT_PM = pm -> pm;
    private static final Function<PackageManager, PackageManager> NO_APP_PREDICTION_SERVICE_PM =
            pm -> {
                PackageManager mock = Mockito.spy(pm);
                when(mock.getAppPredictionServicePackageName()).thenReturn(null);
                return mock;
            };

    @Parameterized.Parameters
    public static Collection packageManagers() {
        return Arrays.asList(new Object[][] {
                {0, "Default PackageManager", DEFAULT_PM},
                {1, "No App Prediction Service", NO_APP_PREDICTION_SERVICE_PM}
        });
    }

    @Override
    protected Intent getConcreteIntentForLaunch(Intent clientIntent) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        clientIntent.setClass(context, com.android.intentresolver.ChooserWrapperActivity.class);

        PackageManager pm = ChooserActivityOverrideData.getInstance().createPackageManager
                .apply(context.getPackageManager());
        clientIntent.putExtra(
                ChooserActivity.EXTRA_IS_APP_PREDICTION_SERVICE_AVAILABLE,
                (pm.getAppPredictionServicePackageName() != null));
        return clientIntent;
    }

    @Override
    protected boolean shouldTestTogglingAppPredictionServiceAvailabilityAtRuntime() {
        // Unbundled chooser takes in app prediction availability as a parameter from the system, so
        // changing the availability conditions after the fact won't make a difference.
        return false;
    }

    @Override
    protected void setup() {
        // TODO: use the other form of `adoptShellPermissionIdentity()` where we explicitly list the
        // permissions we require (which we'll read from the manifest at runtime).
        InstrumentationRegistry
                .getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();

        super.setup();
    }

    public UnbundledChooserActivityTest(
                int testNum,
                String testName,
                Function<PackageManager, PackageManager> packageManagerOverride) {
        super(testNum, testName, packageManagerOverride);
    }

    /* This is a "test of a test" to make sure that our inherited test class
     * is successfully configured to operate on the unbundled-equivalent
     * ChooserWrapperActivity.
     *
     * TODO: remove after unbundling is complete.
     */
    @Test
    public void testWrapperActivityHasExpectedConcreteType() {
        final ChooserActivity activity = mActivityRule.launchActivity(
                Intent.createChooser(new Intent("ACTION_FOO"), "foo"));
        waitForIdle();
        assertThat(activity).isInstanceOf(com.android.intentresolver.ChooserWrapperActivity.class);
    }

    private void waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
