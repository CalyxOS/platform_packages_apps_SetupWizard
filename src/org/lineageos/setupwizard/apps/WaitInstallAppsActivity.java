/*
 * Copyright (C) 2019 The Calyx Institute
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.lineageos.setupwizard.apps;

import static android.view.animation.AnimationUtils.loadAnimation;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_APPS_INSTALLED;

import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;

import org.lineageos.setupwizard.apps.FDroidRepo;
import org.lineageos.setupwizard.BaseSetupWizardActivity;
import org.lineageos.setupwizard.R;

import java.io.IOException;
import java.io.File;

public class WaitInstallAppsActivity extends BaseSetupWizardActivity {

    public static final String TAG = WaitInstallAppsActivity.class.getSimpleName();

    private static final String DEFAULT_BROWSER = "com.duckduckgo.mobile.android";

    private final Handler mHandler = new Handler();

    private ProgressBar mProgressBar;
    private TextView mWaitingForAppsText;

    private String path;

    private final Runnable mDoneWaitingForApps = new Runnable() {
        public void run() {
            // This will be run if apps haven't finished installing in 60 seconds,
            // to prevent getting stuck in SetupWizard.
            setNextAllowed(true);
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_APPS_INSTALLED.equals(intent.getAction())) {
                afterAppsInstalled();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerReceiver(packageReceiver, new IntentFilter(ACTION_APPS_INSTALLED));

        mProgressBar = findViewById(R.id.progress);
        mWaitingForAppsText = findViewById(R.id.waiting_for_apps);
        path = getString(R.string.calyx_fdroid_repo_location);

        if (!shouldWeWaitForApps()) {
            afterAppsInstalled();
        } else {
            // Post this to eventually let the user go next if something goes wrong
            mHandler.postDelayed(mDoneWaitingForApps, 60 * 1000);
            // But first they have to wait for apps to install
            setNextAllowed(false);
            if (!mProgressBar.isShown()) {
                mProgressBar.setVisibility(View.VISIBLE);
                mWaitingForAppsText.setVisibility(View.VISIBLE);
                mProgressBar.startAnimation(loadAnimation(this, R.anim.translucent_enter));
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(packageReceiver);
        super.onDestroy();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.wait_install_apps_activity;
    }

    @Override
    public void onNextPressed() {
        getPackageManager().setDefaultBrowserPackageNameAsUser(DEFAULT_BROWSER, getUserId());
        super.onNextPressed();
    }

    private void afterAppsInstalled() {
        if (mProgressBar.isShown()) {
            mProgressBar.startAnimation(loadAnimation(this, R.anim.translucent_exit));
            mProgressBar.setVisibility(View.INVISIBLE);
            mWaitingForAppsText.setVisibility(View.INVISIBLE);
        }
        setNextAllowed(true);
        onNextPressed();
    }

    private boolean shouldWeWaitForApps() {
        if (AppInstallerService.areAllAppsInstalled())
            return false;
        File repoPath = new File(path);
        if (!repoPath.isDirectory()) {
            return false;
        } else {
            try {
                FDroidRepo.checkFdroidRepo(path);
            } catch (IOException | JSONException e) {
                return false;
            }
        }
        return true;
    }

}
