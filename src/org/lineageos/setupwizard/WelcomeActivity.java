/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_ACCESSIBILITY_SETTINGS;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_EMERGENCY_DIAL;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;
import com.google.android.setupcompat.util.SystemBarHelper;
import com.google.android.setupdesign.gesture.ConsecutiveTapsGestureDetector;

import org.lineageos.setupwizard.util.SetupWizardUtils;

import java.util.concurrent.TimeUnit;

public class WelcomeActivity extends SubBaseActivity {

    private ConsecutiveTapsGestureDetector mConsecutiveTapsGestureDetector;
    private GestureDetector mGestureDetector;

    @Override
    protected void onStartSubactivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBarHelper.setBackButtonVisible(getWindow(), false);
        ImageView brandLogoView = findViewById(R.id.brand_logo);
        FrameLayout page = findViewById(R.id.page);
        setNextText(R.string.start);
        Button startButton = findViewById(R.id.start);
        Button emergButton = findViewById(R.id.emerg_dialer);
        startButton.setOnClickListener(view -> onNextPressed());
        findViewById(R.id.launch_accessibility)
                .setOnClickListener(
                        view -> startSubactivity(new Intent(ACTION_ACCESSIBILITY_SETTINGS)));

        FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(this, startButton, true);

        if (SetupWizardUtils.hasTelephony(this)) {
            setSkipText(R.string.emergency_call);
            emergButton.setOnClickListener(
                    view -> startSubactivity(new Intent(ACTION_EMERGENCY_DIAL)));

            FooterButtonStyleUtils.applySecondaryButtonPartnerResource(this, emergButton, true);
        } else {
            emergButton.setVisibility(View.GONE);
        }

        TextView welcomeTitle = findViewById(R.id.welcome_title);
        if (SetupWizardUtils.isManagedProfile(this)) {
            welcomeTitle.setText(getString(R.string.setup_managed_profile_welcome_message));
        } else {
            welcomeTitle.setText(getString(R.string.setup_welcome_message,
                    getString(R.string.os_name)));
        }

        if (Build.IS_DEBUGGABLE) {
            mConsecutiveTapsGestureDetector = new ConsecutiveTapsGestureDetector(
                    (ConsecutiveTapsGestureDetector.OnConsecutiveTapsListener)
                            numOfConsecutiveTaps -> {
                                if (numOfConsecutiveTaps == 4) {
                                    Toast.makeText(WelcomeActivity.this, R.string.skip_setupwizard,
                                            Toast.LENGTH_LONG).show();
                                    SetupWizardUtils.finishSetupWizard(WelcomeActivity.this);
                                }
                            }, findViewById(R.id.setup_wizard_layout),
                    (int) TimeUnit.SECONDS.toMillis(1));
        }
        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        Rect viewRect = new Rect();
                        int[] leftTop = new int[2];

                        Button factoryResetButton = findViewById(R.id.factory_reset);
                        factoryResetButton.setOnClickListener(
                                v -> factoryResetAndShutdown());
                        FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(
                                WelcomeActivity.this, factoryResetButton, true);

                        brandLogoView.getLocationOnScreen(leftTop);
                        viewRect.set(leftTop[0], leftTop[1],
                                leftTop[0] + brandLogoView.getWidth(),
                                leftTop[1] + brandLogoView.getHeight());
                        if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                            setupDetails();
                            page.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (Build.IS_DEBUGGABLE) {
            mConsecutiveTapsGestureDetector.onTouchEvent(ev);
        }
        mGestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.welcome_activity;
    }

    @Override
    protected int getTitleResId() {
        return -1;
    }

    private void setupDetails() {
        // CalyxOS is meant to be used with a locked bootloader and OEM Unlocking disabled
        final boolean bootloaderUnlocked = SetupWizardUtils.isBootloaderUnlocked(this);
        final boolean oemunlockAllowed = SetupWizardUtils.isOemunlockAllowed(this);
        final TextView bootloaderStatus = (TextView) findViewById(R.id.bootloader_status);
        final TextView oemunlockStatus = (TextView) findViewById(R.id.oemunlock_status);

        if (bootloaderUnlocked) {
            // Bootloader unlocked, bad.
            bootloaderStatus.setText(R.string.bootloader_unlocked);
            bootloaderStatus.setTextColor(getColor(R.color.red));
            // OEM Unlocking is greyed out when bootloader is unlocked
            oemunlockStatus.setText(R.string.oemunlock_na);
            oemunlockStatus.setTextColor(getColor(R.color.yellow));
        } else {
            // Bootloader locked, good.
            bootloaderStatus.setText(R.string.bootloader_locked);
            bootloaderStatus.setTextColor(getColor(R.color.green));
            if (oemunlockAllowed) {
                // OEM Unlocking allowed, bad.
                oemunlockStatus.setText(R.string.oemunlock_allowed);
                oemunlockStatus.setTextColor(getColor(R.color.red));
            } else {
                // OEM Unlocking not allowed, good.
                oemunlockStatus.setText(R.string.oemunlock_notallowed);
                oemunlockStatus.setTextColor(getColor(R.color.green));
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
