/*
 * SPDX-FileCopyrightText: 2021-2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import static org.lineageos.setupwizard.util.SetupWizardUtils.isBootloaderUnlocked;

import android.annotation.Nullable;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Button;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;

public class BootloaderWarningActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isBootloaderUnlocked(this) || Build.IS_DEBUGGABLE) {
            nextAction(RESULT_SKIP);
            return;
        }
        findViewById(R.id.reboot_bootloader).setOnClickListener(v -> {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("bootloader");
        });
        Button rebootButton = findViewById(R.id.reboot_bootloader);
        FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(this, rebootButton, true);
        getGlifLayout().setDescriptionText(getString(R.string.bootloader_warning_summary));
        setNextAllowed(false);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.bootloader_warning_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.bootloader_warning_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.bootloader_warning_icon;
    }

}
