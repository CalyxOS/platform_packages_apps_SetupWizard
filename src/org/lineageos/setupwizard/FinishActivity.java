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

import com.google.android.setupcompat.util.SystemBarHelper;

import org.lineageos.setupwizard.util.ManagedProvisioningUtils;
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.ProvisioningState;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();
    // Provisioning finalization relaunches our activity, interrupting the animation. Not cool.
    // Unclear what to do other than wait.
    private static final int WAIT_FOR_FINALIZATION_MS = 1000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private enum FinishState {
        NONE,
        PROVISIONING,
        PREFINALIZATION,
        FINALIZING_PROVISIONING,
        WAITING_FOR_FINALIZATION,
        SHOULD_ANIMATE,
        ANIMATING,
        FINISHED
    }

    // "Why not just start this activity with an Intent extra?" you might ask. Been there.
    // We need this to affect the theme, and even onCreate is not early enough for that,
    // so "static volatile" it is. Feel free to rework this if you dare.
    private static volatile FinishState sFinishState = FinishState.NONE;

    private View mRootView;
    private Resources.Theme mEdgeToEdgeWallpaperBackgroundTheme;

    private ActivityResultLauncher<Intent> mStartProvisioningResultLauncher;
    private ActivityResultLauncher<Intent> mFinalizeProvisioningResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate: sFinishState=" + sFinishState);

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

        if (sFinishState != FinishState.NONE) {
            disableNavigation();
        }

        if (ManagedProvisioningUtils.maybeShowFailedProvisioningDialogAgain(this)) {
            return;
        }

        switch (sFinishState) {
            case NONE:
                break;
            case WAITING_FOR_FINALIZATION:
                // As long as provisioning keeps relaunching us, we need to keep delaying.
                mHandler.removeCallbacksAndMessages(/* token */ this);
                mHandler.postDelayed(this::relaunchAndRunAnimation, /* token */ this,
                        WAIT_FOR_FINALIZATION_MS);
                break;
            case SHOULD_ANIMATE:
                mHandler.removeCallbacksAndMessages(/* token */ this);
                startFinishSequence();
                break;
            case FINISHED:
                Log.e(TAG, "Should not start again when finished!");
                finish();
                break;
            default:
                Log.w(TAG, "Unexpected onCreate state " + sFinishState);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: sFinishState=" + sFinishState);
        mStartProvisioningResultLauncher.unregister();
        mFinalizeProvisioningResultLauncher.unregister();
        mHandler.removeCallbacksAndMessages(/* token */ this);
    }

    private void disableNavigation() {
        hideNextButton();
        SystemBarHelper.setBackButtonVisible(getWindow(), false);
    }

    private void disableActivityTransitions() {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
    }

    @Override
    protected void applyForwardTransition() {
        if (FinishState.NONE == sFinishState) {
            super.applyForwardTransition();
        }
    }

    @Override
    protected void applyBackwardTransition() {
        if (FinishState.NONE == sFinishState) {
            super.applyBackwardTransition();
        }
    }

    private void finalizeProvisioning() {
        final ProvisioningState provisioningState = getProvisioningState(this);
        if (LOGV) {
            Log.v(TAG, "finalizeProvisioning, provisioningState=" + provisioningState);
        }
        switch (provisioningState) {
            case COMPLETE:
                // Expected. Do nothing.
                break;
            case UNSUPPORTED:
                Log.e(TAG, "Tried to finalize provisioning when unsupported");
                ManagedProvisioningUtils.showFailedProvisioningDialog(this);
            case PENDING:
            case FINALIZED:
                Log.w(TAG, "finalizeProvisioning, unexpected state " + provisioningState);
                break;
        }
        if (sFinishState != FinishState.PROVISIONING) {
            Log.e(TAG, "Tried to finalize provisioning when " + sFinishState);
            ManagedProvisioningUtils.showFailedProvisioningDialog(this);
        }
        disableNavigation();
        sFinishState = FinishState.PREFINALIZATION;
        ManagedProvisioningUtils.installManagedProfileApps(this, result -> {
            if (result != null) {
                Log.e(TAG, "Failed to finalize provisioning", result);
                ManagedProvisioningUtils.showFailedProvisioningDialog(this);
            } else {
                sFinishState = FinishState.FINALIZING_PROVISIONING;
                mFinalizeProvisioningResultLauncher.launch(getFinalizeProvisioningIntent(this));
            }
        });
    }

    private void onStartProvisioningResult(ActivityResult activityResult) {
        try {
            finalizeProvisioning();
        } catch (Exception e) {
            Log.e(TAG, "Failed to finalize provisioning", e);
            ManagedProvisioningUtils.showFailedProvisioningDialog(this);
        }
    }

    private void onFinalizeProvisioningResult(ActivityResult activityResult) {
        if (sFinishState != FinishState.FINALIZING_PROVISIONING) {
            Log.e(TAG, "onFinalizeProvisioningResult: Unexpected sFinishState " + sFinishState);
            ManagedProvisioningUtils.showFailedProvisioningDialog(this);
            return;
        }
        final ProvisioningState provisioningState = getProvisioningState(this);
        if (provisioningState != ProvisioningState.FINALIZED) {
            Log.e(TAG, "onFinalizeProvisioningResult: expected FINALIZED, got "
                    + provisioningState);
            Log.e(TAG, "onFinalizeProvisioningResult: activityResult=" + activityResult);
            ManagedProvisioningUtils.showFailedProvisioningDialog(this);
            return;
        }
        sFinishState = FinishState.WAITING_FOR_FINALIZATION;
        mHandler.postDelayed(this::relaunchAndRunAnimation, /* token */ this,
                WAIT_FOR_FINALIZATION_MS);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (sFinishState == FinishState.NONE) {
            return theme;
        }
        if (mEdgeToEdgeWallpaperBackgroundTheme == null) {
            theme.applyStyle(R.style.EdgeToEdgeWallpaperBackground, true);
            mEdgeToEdgeWallpaperBackgroundTheme = theme;
        }
        return mEdgeToEdgeWallpaperBackgroundTheme;
    }

    @Override
    public void onNavigateNext() {
        if (FinishState.NONE == sFinishState
                && ProvisioningState.PENDING == getProvisioningState(this)) {
            // Initialize garlic-level provisioning.
            sFinishState = FinishState.PROVISIONING;
            mStartProvisioningResultLauncher.launch(getStartProvisioningIntent(this));
            // Do not finish yet...
            return;
        }
        switch (sFinishState) {
            case NONE:
                relaunchAndRunAnimation();
                break;
            default:
                Log.e(TAG, "Unexpected state " + sFinishState + " when navigating next");
        }
    }

    private void relaunchAndRunAnimation() {
        sFinishState = FinishState.SHOULD_ANIMATE;
        // Relaunching the activity before finishing is the only way currently known to prevent
        // an out-of-place slide transition from happening, even when disabling transitions, and
        // regardless of when we disable them. This also means we can't simply call recreate(), but
        // another reason is that recreate() doesn't seem to reinitialize the theme, which is the
        // entire point of relaunching - to ensure this activity reveals a wallpaper background.
        // These theme shenanigans and relaunching were not necessary prior to Android 14 QPR3.
        startActivity(getIntent());
        finish();
        disableActivityTransitions();
    }

    private void startFinishSequence() {
        sFinishState = FinishState.ANIMATING;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        disableNavigation();

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
        if (sFinishState != FinishState.ANIMATING) {
            Log.e(TAG, "animateOut but in " + sFinishState + " phase. How?");
            return;
        }
        final int cx = (mRootView.getLeft() + mRootView.getRight()) / 2;
        final int cy = (mRootView.getTop() + mRootView.getBottom()) / 2;
        final float fullRadius = (float) Math.hypot(cx, cy);
        Animator anim;
        try {
            anim = ViewAnimationUtils.createCircularReveal(mRootView, cx, cy, fullRadius, 0f);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to create finish animation", e);
            finishAfterAnimation();
            return;
        }
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

    private void finishAfterAnimation() {
        if (ProvisioningState.FINALIZED == getProvisioningState(this)) {
            // Start the Setup Wizard within the finalized managed profile.
            startActivityAsUser(new Intent(this, SetupWizardActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK), UserManager.get(this).getUserHandles(
                    false).getLast());
        }
        SetupWizardUtils.finishSetupWizard(FinishActivity.this);
        sFinishState = FinishState.FINISHED;
    }
}
