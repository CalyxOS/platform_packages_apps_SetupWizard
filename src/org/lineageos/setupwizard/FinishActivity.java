/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2020, 2022 The LineageOS Project
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

import static android.os.Binder.getCallingUserHandle;
import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static org.lineageos.setupwizard.Manifest.permission.FINISH_SETUP;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_FINISHED;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_SETUP_COMPLETE;
import static org.lineageos.setupwizard.SetupWizardApp.LOGV;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.util.ManagedProvisioningUtils;
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.ProvisioningState;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private ImageView mReveal;

    private SetupWizardApp mSetupWizardApp;

    private final Handler mHandler = new Handler();

    private boolean mIsFinishing;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOGV) {
                Log.v(TAG, "onReceive intent=" + intent);
            }
            if (intent != null && intent.getAction() == ACTION_FINISHED) {
                unregisterReceiver(mIntentReceiver);
                completeSetup();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        mSetupWizardApp = (SetupWizardApp) getApplication();
        mReveal = (ImageView) findViewById(R.id.reveal);
        setNextText(R.string.start);
    }

    @Override
    public void onBackPressed() {
        if (!mIsFinishing) {
            super.onBackPressed();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public void onNavigateNext() {
        startFinishSequence();
    }

    private void startFinishSequence() {
        if (mIsFinishing) {
            return;
        }
        ProvisioningState provisioningState =
                ManagedProvisioningUtils.getProvisioningState(this);
        if (ManagedProvisioningUtils.getProvisioningState(this) == ProvisioningState.PENDING) {
            // Initialize garlic-level provisioning.
            ManagedProvisioningUtils.init(this);
            // Do not finish yet...
            return;
        }
        mIsFinishing = true;

        // Listen for completion from the exit service.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FINISHED);
        registerReceiver(mIntentReceiver, filter, null, null);

        mSetupWizardApp.provisionDefaultUserAppPermissions();
        Intent i = new Intent(ACTION_SETUP_COMPLETE);
        i.setPackage(getPackageName());
        sendBroadcastAsUser(i, getCallingUserHandle(), FINISH_SETUP);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        hideNextButton();

        // Begin outro animation.
        setupRevealImage();
    }

    private void setupRevealImage() {
        // Edge-to-edge.
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.navigationBars());
        }

        final Point p = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(p);
        final WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(this);
        wallpaperManager.forgetLoadedWallpaper();
        final Bitmap wallpaper = wallpaperManager.getBitmap();
        Bitmap cropped = null;
        if (wallpaper != null) {
            cropped = Bitmap.createBitmap(wallpaper, 0,
                    0, Math.min(p.x, wallpaper.getWidth()),
                    Math.min(p.y, wallpaper.getHeight()));
        }
        if (cropped != null) {
            mReveal.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mReveal.setImageBitmap(cropped);
        } else {
            mReveal.setBackground(wallpaperManager
                    .getBuiltInDrawable(p.x, p.y, false, 0, 0));
        }
        animateOut();
    }

    private void animateOut() {
        int cx = (mReveal.getLeft() + mReveal.getRight()) / 2;
        int cy = (mReveal.getTop() + mReveal.getBottom()) / 2;
        int finalRadius = Math.max(mReveal.getWidth(), mReveal.getHeight());
        Animator anim =
                ViewAnimationUtils.createCircularReveal(mReveal, cx, cy, 0, finalRadius);
        anim.setDuration(900);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mReveal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (LOGV) {
                            Log.v(TAG, "Animation ended");
                        }
                        // Start exit procedures, including the exit service.
                        SetupWizardUtils.startSetupWizardExitProcedure(FinishActivity.this);
                    }
                });
            }
        });
        anim.start();
    }

    private void completeSetup() {
        Log.i(TAG, "Setup complete!");
        handleNavigationOption(mSetupWizardApp);
        final WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(mSetupWizardApp);
        wallpaperManager.forgetLoadedWallpaper();
        finishAllAppTasks();
        overridePendingTransition(R.anim.translucent_enter, R.anim.translucent_exit);
    }

    private void handleNavigationOption(Context context) {
        Bundle settingsBundle = mSetupWizardApp.getSettingsBundle();
        if (settingsBundle.containsKey(NAVIGATION_OPTION_KEY)) {
            IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            String selectedNavMode = settingsBundle.getString(NAVIGATION_OPTION_KEY);

            try {
                overlayManager.setEnabledExclusiveInCategory(selectedNavMode, USER_CURRENT);
            } catch (Exception e) {}
        }
    }
}
