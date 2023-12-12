/*
 * Copyright (C) 2023 The Calyx Institute
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

package org.lineageos.setupwizard.util;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;

import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_SAFER;
import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_SAFEST;
import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_STANDARD;
import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.util.Log;

import lineageos.providers.LineageSettings;

import org.lineageos.setupwizard.R;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class GarlicLevelHelper {

    private static final String TAG = GarlicLevelHelper.class.getSimpleName();
    // sync with com.android.settings.password.ChooseLockSettingsHelper
    private static final String EXTRA_KEY_REQUESTED_MIN_COMPLEXITY = "requested_min_complexity";

    private Context mContext;

    public GarlicLevelHelper(Context context) {
        mContext = context;
    }

    private int getGarlicLevel() {
        try {
            return LineageSettings.Global.getInt(mContext.getContentResolver(),
                    LineageSettings.Global.GARLIC_LEVEL);
        } catch (LineageSettings.LineageSettingNotFoundException ignored) {
            return GARLIC_LEVEL_STANDARD;
        }
    }

    private static int getGarlicLevelMinPasswordComplexity(int garlicLevel) {
        switch (garlicLevel) {
            case GARLIC_LEVEL_SAFER:
                return PASSWORD_COMPLEXITY_LOW;
            case GARLIC_LEVEL_SAFEST:
                return PASSWORD_COMPLEXITY_MEDIUM;
            default:
                return PASSWORD_COMPLEXITY_NONE;
        }
    }

    public Intent putMinPasswordComplexityToIntent(Intent intent) {
        int garlicLevel = getGarlicLevel();
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY,
                getGarlicLevelMinPasswordComplexity(garlicLevel));
        return intent;
    }

    /**
      * Return the current password complexity if it is not sufficient for the current garlic level.
      * Otherwise, return null.
      */
    private Integer getPasswordComplexityIfUnmetForGarlicLevel() {
        if (!SetupWizardUtils.isOwner()) {
            // This requirement only applies to the owner user.
            return null;
        }
        int garlicLevel = getGarlicLevel();
        int minComplexity = getGarlicLevelMinPasswordComplexity(garlicLevel);
        if (minComplexity == PASSWORD_COMPLEXITY_NONE) {
            return null;
        }
        int complexity = mContext.getSystemService(DevicePolicyManager.class)
                .getPasswordComplexity();
        if (LOGV) Log.v(TAG, "Password complexity requirement: minComplexity: " + minComplexity
                + ", complexity: " + complexity);
        // As of this writing, password complexity values may be compared.
        return (complexity >= minComplexity) ? null : complexity;
    }

    /**
      * If the current password complexity is not sufficient for the current garlic level,
      * show an alert dialog. When dismissed, use the provided onDismissListener.
      * Return true if the password was insufficient and a dialog was shown. Otherwise, false.
      */
    public boolean maybeShowInsufficientPasswordDialog(OnDismissListener onDismissListener) {
        final Integer passwordComplexity = getPasswordComplexityIfUnmetForGarlicLevel();
        if (passwordComplexity == null) {
            return false;
        }
        if (passwordComplexity != PASSWORD_COMPLEXITY_NONE) {
            Log.wtf(TAG, "Insufficient screen lock provided. This should not be possible, as the "
                    + "chooser activity is supposed to enforce the requested complexity.");
        }
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.security_level_password_required_title)
                .setMessage(R.string.security_level_password_required_desc)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(onDismissListener)
                .create();
        dialog.show();
        return true;
    }
}
