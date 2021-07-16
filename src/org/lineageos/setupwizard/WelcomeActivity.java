/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2021 The LineageOS Project
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

package org.lineageos.setupwizard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.setupcompat.util.SystemBarHelper;

import org.lineageos.setupwizard.util.SetupWizardUtils;

import java.util.concurrent.TimeUnit;

public class WelcomeActivity extends BaseSetupWizardActivity {

    public static final String TAG = WelcomeActivity.class.getSimpleName();

    private View mRootView;
    private ImageView mBrandLogoView;
    private FrameLayout mPage;
    private GestureDetector mGestureDetector;
    private MotionEvent previousTapEvent;
    private int consecutiveTaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBarHelper.setBackButtonVisible(getWindow(), false);
        mRootView = findViewById(R.id.root_view);
        mBrandLogoView = findViewById(R.id.brand_logo);
        mPage = findViewById(R.id.page);
        setNextText(R.string.start);
        setSkipText(R.string.emergency_call);
        findViewById(R.id.start).setOnClickListener(view -> onNextPressed());
        findViewById(R.id.emerg_dialer)
                .setOnClickListener(view -> startEmergencyDialer());
        findViewById(R.id.launch_accessibility)
                .setOnClickListener(view -> startAccessibilitySettings());
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Rect viewRect = new Rect();
                int[] leftTop = new int[2];
                mRootView.getLocationOnScreen(leftTop);
                viewRect.set(
                        leftTop[0], leftTop[1], leftTop[0] + mRootView.getWidth(), leftTop[1]
                        + mRootView.getHeight());
                if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                    if (isConsecutiveTap(e)) {
                        consecutiveTaps++;
                    } else {
                        consecutiveTaps = 1;
                    }
                    if (Build.IS_DEBUGGABLE && consecutiveTaps == 4) {
                        Toast.makeText(WelcomeActivity.this, R.string.skip_setupwizard,
                                Toast.LENGTH_LONG).show();
                        SetupWizardUtils.finishSetupWizard(WelcomeActivity.this);
                    }
                } else {
                    // Touch outside the target view. Reset counter.
                    consecutiveTaps = 0;
                }

                if (previousTapEvent != null) {
                    previousTapEvent.recycle();
                }
                previousTapEvent = MotionEvent.obtain(e);
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Rect viewRect = new Rect();
                int[] leftTop = new int[2];

                findViewById(R.id.factory_reset).setOnClickListener(v -> factoryResetAndShutdown());

                mBrandLogoView.getLocationOnScreen(leftTop);
                viewRect.set(leftTop[0], leftTop[1], leftTop[0] + mBrandLogoView.getWidth(),
                        leftTop[1] + mBrandLogoView.getHeight());
                if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                    setupDetails();
                    mPage.setVisibility(View.VISIBLE);
                }
            }
        });

        mRootView.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.welcome_activity;
    }

    private boolean isConsecutiveTap(MotionEvent currentTapEvent) {
        if (previousTapEvent == null) {
            return false;
        }

        double deltaX = previousTapEvent.getX() - currentTapEvent.getX();
        double deltaY = previousTapEvent.getY() - currentTapEvent.getY();
        long deltaTime = currentTapEvent.getEventTime() - previousTapEvent.getEventTime();
        return (deltaX * deltaX + deltaY * deltaY >=
                (mRootView.getWidth() * mRootView.getWidth()) / 2
                && deltaTime < TimeUnit.SECONDS.toMillis(1));
    }

    private void setupDetails() {
        // CalyxOS is meant to be used with a locked bootloader and OEM Unlocking disabled
        final Boolean bootloaderUnlocked = SetupWizardUtils.isBootloaderUnlocked(this);
        final Boolean oemunlockAllowed = SetupWizardUtils.isOemunlockAllowed(this);
        final TextView bootloaderStatus = (TextView) findViewById(R.id.bootloader_status);
        final TextView oemunlockStatus = (TextView) findViewById(R.id.oemunlock_status);

        if (bootloaderUnlocked) {
            // Bootloader unlocked, bad.
            bootloaderStatus.setText(R.string.bootloader_unlocked);
            bootloaderStatus.setTextColor(Color.RED);
            // OEM Unlocking is greyed out when bootloader is unlocked
            oemunlockStatus.setText(R.string.oemunlock_na);
            oemunlockStatus.setTextColor(Color.YELLOW);
        } else {
            // Bootloader locked, good.
            bootloaderStatus.setText(R.string.bootloader_locked);
            bootloaderStatus.setTextColor(Color.GREEN);
            if (oemunlockAllowed) {
                // OEM Unlocking allowed, bad.
                oemunlockStatus.setText(R.string.oemunlock_allowed);
                oemunlockStatus.setTextColor(Color.RED);
            } else {
                // OEM Unlocking not allowed, good.
                oemunlockStatus.setText(R.string.oemunlock_notallowed);
                oemunlockStatus.setTextColor(Color.GREEN);
            }
        }
    }

    private void factoryResetAndShutdown() {
        // com.android.settings.MasterClearConfirm.doMasterClear()
        Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
        intent.setPackage("android");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_REASON, "SetupWizard");
        intent.putExtra(Intent.EXTRA_WIPE_ESIMS, false);
        // com.android.server.MasterClearReceiver
        intent.putExtra("shutdown", true);
        sendBroadcast(intent);
        // Intent handling is asynchronous -- assume it will happen soon.
    }
}
