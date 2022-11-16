/*
 * Copyright (C) 2022 The Calyx Institute
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

import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_SAFER;
import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_SAFEST;
import static org.lineageos.setupwizard.SetupWizardApp.GARLIC_LEVEL_STANDARD;

import android.os.Bundle;
import android.widget.RadioGroup;

import lineageos.providers.LineageSettings;

public class GarlicLevelActivity extends BaseSetupWizardActivity {

    private int mSelection = GARLIC_LEVEL_STANDARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getGlifLayout().setDescriptionText(getString(R.string.security_level_subtitle));

        final RadioGroup radioGroup = findViewById(R.id.garlic_radio_group);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_standard:
                    mSelection = GARLIC_LEVEL_STANDARD;
                    break;
                case R.id.radio_safer:
                    mSelection = GARLIC_LEVEL_SAFER;
                    break;
                case R.id.radio_safest:
                    mSelection = GARLIC_LEVEL_SAFEST;
                    break;
            }
        });
    }

    @Override
    protected void onNextPressed() {
        super.onNextPressed();
        setGarlicLevel();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.garlic_level_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.security_level_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_shield;
    }

    private void setGarlicLevel() {
        LineageSettings.Global.putInt(getContentResolver(),
                LineageSettings.Global.GARLIC_LEVEL, mSelection);
    }
}