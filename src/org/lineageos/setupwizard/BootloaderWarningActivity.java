/*
 * Copyright (C) 2021-2022 The Calyx Institute
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

import android.annotation.Nullable;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;

public class BootloaderWarningActivity extends BaseSetupWizardActivity {

    public static final String TAG = BootloaderWarningActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.reboot_bootloader).setOnClickListener(v -> {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("bootloader");
        });
        setNextText(R.string.skip);
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
