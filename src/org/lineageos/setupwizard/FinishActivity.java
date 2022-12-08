/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;
import static org.lineageos.setupwizard.util.ManagedProvisioningUtils.getFinalizeProvisioningIntent;
import static org.lineageos.setupwizard.util.ManagedProvisioningUtils.getProvisioningState;
import static org.lineageos.setupwizard.util.ManagedProvisioningUtils.getStartProvisioningIntent;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.lineageos.setupwizard.util.ManagedProvisioningUtils;
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.ProvisioningState;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // "Why not just start this activity with an Intent extra?" you might ask. Been there.
    // We need this to affect the theme, and even onCreate is not early enough for that,
    // so "static volatile boolean" it is. Feel free to rework this if you dare.
    private static volatile boolean sIsFinishing;

    private View mRootView;
    private Resources.Theme mEdgeToEdgeWallpaperBackgroundTheme;

    private ActivityResultLauncher<Intent> mStartProvisioningResultLauncher;
    private ActivityResultLauncher<Intent> mFinalizeProvisioningResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartProvisioningResultLauncher = registerForActivityResult(
                new StartDecoratedActivityForResult(),
                this::onStartProvisioningResult);
        mFinalizeProvisioningResultLauncher = registerForActivityResult(
                new StartDecoratedActivityForResult(),
                this::onFinalizeProvisioningResult);

        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.translucent_enter,
                R.anim.translucent_exit);
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        setNextText(R.string.start);

        // Edge-to-edge. Needed for the background view to fill the full screen.
        final Window window = getWindow();
        window.setDecorFitsSystemWindows(false);

        // Make sure 3-button navigation bar is the same color as the rest of the screen.
        window.setNavigationBarContrastEnforced(false);

        // Ensure the main layout (not including the background view) does not get obscured by bars.
        mRootView = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(mRootView, (view, windowInsets) -> {
            final View linearLayout = findViewById(R.id.linear_layout);
            final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            final MarginLayoutParams params = (MarginLayoutParams) linearLayout.getLayoutParams();
            params.leftMargin = insets.left;
            params.topMargin = insets.top;
            params.rightMargin = insets.right;
            params.bottomMargin = insets.bottom;
            linearLayout.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });

        if (sIsFinishing) {
            startFinishSequence();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStartProvisioningResultLauncher.unregister();
        mFinalizeProvisioningResultLauncher.unregister();
    }

    private void disableActivityTransitions() {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
    }

    @Override
    protected void applyForwardTransition() {
        if (!sIsFinishing) {
            super.applyForwardTransition();
        }
    }

    @Override
    protected void applyBackwardTransition() {
        if (!sIsFinishing) {
            super.applyBackwardTransition();
        }
    }

    private void finishAfterAnimation() {
        if (!maybeFinalizeProvisioning()) {
            // If provisioning is being finalized, it will finish when it's done.
            SetupWizardUtils.finishSetupWizard(FinishActivity.this);
        }
    }

    private boolean maybeFinalizeProvisioning() {
        final ProvisioningState provisioningState = getProvisioningState(this);
        if (LOGV) {
            Log.v(TAG, "maybeFinalizeProvisioning, provisioningState=" + provisioningState);
        }
        if (provisioningState == ProvisioningState.PENDING) {
            Log.e(TAG, "maybeFinalizeProvisioning, but provisioning pending! Murky waters ahead!");
        }
        if (provisioningState != ProvisioningState.COMPLETE) {
            return false;
        }
        if (!SetupWizardUtils.isUserSetupMarkedComplete(this)) {
            SetupWizardUtils.markUserSetupComplete(this);
            ManagedProvisioningUtils.installManagedProfileApps(this, result -> {
                if (result != null) {
                    Log.e(TAG, "Failed to finalize provisioning", result);
                    ManagedProvisioningUtils.showFailedProvisioningDialog(this);
                } else {
                    mFinalizeProvisioningResultLauncher.launch(getFinalizeProvisioningIntent(this));
                }
            });
            return true;
        }
        return false;
    }

    private void onStartProvisioningResult(ActivityResult activityResult) {
        // do nothing
    }

    private void onFinalizeProvisioningResult(ActivityResult activityResult) {
        startActivityAsUser(new Intent(this, SetupWizardActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK), UserManager.get(this).getUserHandles(
                false).getLast());
        SetupWizardUtils.finishSetupWizard(FinishActivity.this);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (sIsFinishing) {
            if (mEdgeToEdgeWallpaperBackgroundTheme == null) {
                theme.applyStyle(R.style.EdgeToEdgeWallpaperBackground, true);
                mEdgeToEdgeWallpaperBackgroundTheme = theme;
            }
            return mEdgeToEdgeWallpaperBackgroundTheme;
        }
        return theme;
    }

    @Override
    public void onNavigateNext() {
        if (ProvisioningState.PENDING == getProvisioningState(this)) {
            // Initialize garlic-level provisioning.
            ManagedProvisioningUtils.init(this);
            mStartProvisioningResultLauncher.launch(getStartProvisioningIntent(this));
            // Do not finish yet...
            return;
        }
        if (!sIsFinishing) {
            sIsFinishing = true;
            startActivity(getIntent());
            finish();
            disableActivityTransitions();
        }
        hideNextButton();
    }

    private void startFinishSequence() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        hideNextButton();

        // Begin outro animation.
        if (mRootView.isAttachedToWindow()) {
            mHandler.post(() -> animateOut());
        } else {
            mRootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    mHandler.post(() -> animateOut());
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    // Do nothing
                }
            });
        }
    }

    private void animateOut() {
        final int cx = (mRootView.getLeft() + mRootView.getRight()) / 2;
        final int cy = (mRootView.getTop() + mRootView.getBottom()) / 2;
        final float fullRadius = (float) Math.hypot(cx, cy);
        Animator anim =
                ViewAnimationUtils.createCircularReveal(mRootView, cx, cy, fullRadius, 0f);
        anim.setDuration(900);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRootView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRootView.setVisibility(View.INVISIBLE);
                mHandler.post(() -> {
                    if (LOGV) {
                        Log.v(TAG, "Animation ended");
                    }
                    finishAfterAnimation();
                });
            }
        });
        anim.start();
    }
}
