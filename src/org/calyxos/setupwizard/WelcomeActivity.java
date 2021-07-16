/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2018,2020 The LineageOS Project
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

package org.calyxos.setupwizard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.calyxos.setupwizard.util.EnableAccessibilityController;
import org.calyxos.setupwizard.util.SetupWizardUtils;

import java.util.concurrent.TimeUnit;

public class WelcomeActivity extends BaseSetupWizardActivity {

    public static final String TAG = WelcomeActivity.class.getSimpleName();

    private View mRootView;
    private ImageView mBrandLogoView;
    private FrameLayout mPage;
    private EnableAccessibilityController mEnableAccessibilityController;
    private GestureDetector mGestureDetector;
    private MotionEvent previousTapEvent;
    private int consecutiveTaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = findViewById(R.id.root);
        mBrandLogoView = findViewById(R.id.brand_logo);
        mPage = findViewById(R.id.page);
        setNextText(R.string.next);
        setBackText(R.string.emergency_call);
        setBackDrawable(null);
        mEnableAccessibilityController =
                EnableAccessibilityController.getInstance(getApplicationContext());
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
                        leftTop[0], leftTop[1], leftTop[0] + mRootView.getWidth(), leftTop[1] + mRootView.getHeight());
                if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                    if (isConsecutiveTap(e)) {
                        consecutiveTaps++;
                    } else {
                        consecutiveTaps = 1;
                    }
                    if (consecutiveTaps == 4) {
                        Toast.makeText(WelcomeActivity.this, R.string.skip_setupwizard, Toast.LENGTH_LONG).show();
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
                viewRect.set(
                        leftTop[0], leftTop[1], leftTop[0] + mBrandLogoView.getWidth(), leftTop[1] + mBrandLogoView.getHeight());
                if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                    setupDetails();
                    mPage.setVisibility(View.VISIBLE);
                }
            }
        });

        mRootView.setOnTouchListener((v, event) ->
                mEnableAccessibilityController.onTouchEvent(event));
        mRootView.setOnTouchListener((v, event) ->
                mGestureDetector.onTouchEvent(event));
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onNavigateBack() {
        startEmergencyDialer();
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
        return (deltaX * deltaX + deltaY * deltaY >= (mRootView.getWidth() * mRootView.getWidth()) / 2
                && deltaTime < TimeUnit.SECONDS.toMillis(1));
    }

    private void setupDetails() {
        // CalyxOS is meant to be used with a locked bootloader and OEM Unlocking disabled
        final Boolean bootloaderUnlocked = SetupWizardUtils.isBootloaderUnlocked(this);
        final TextView bootloaderStatus = (TextView) findViewById(R.id.bootloader_status);
        if (bootloaderUnlocked) {
            bootloaderStatus.setText(R.string.bootloader_unlocked);
            bootloaderStatus.setTextColor(Color.RED);
        } else {
            bootloaderStatus.setText(R.string.bootloader_locked);
            bootloaderStatus.setTextColor(Color.GREEN);
        }

        final Boolean oemunlockAllowed = SetupWizardUtils.isOemunlockAllowed(this);
        final TextView oemunlockStatus = (TextView) findViewById(R.id.oemunlock_status);
        if (oemunlockAllowed) {
            oemunlockStatus.setText(R.string.oemunlock_allowed);
            oemunlockStatus.setTextColor(Color.GREEN);
        } else {
            oemunlockStatus.setText(R.string.oemunlock_notallowed);
            oemunlockStatus.setTextColor(Color.RED);
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
