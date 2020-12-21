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

import android.annotation.Nullable;
import android.os.Bundle;

public class BootloaderWarningActivity extends BaseSetupWizardActivity {

    public static final String TAG = BootloaderWarningActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNextText(R.string.reboot);
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
