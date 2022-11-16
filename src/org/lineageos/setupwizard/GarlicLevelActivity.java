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
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

import lineageos.providers.LineageSettings;

public class GarlicLevelActivity extends BaseSetupWizardActivity {

    private int mSelection = GARLIC_LEVEL_STANDARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getGlifLayout().setDescriptionText(getString(R.string.security_level_subtitle));

        findViewById(R.id.standard).setOnClickListener((view) -> {
            mSelection = GARLIC_LEVEL_STANDARD;
            highlightView(view);
        });

        findViewById(R.id.safer).setOnClickListener((view) -> {
            mSelection = GARLIC_LEVEL_SAFER;
            highlightView(view);
        });

        findViewById(R.id.safest).setOnClickListener((view) -> {
            mSelection = GARLIC_LEVEL_SAFEST;
            highlightView(view);
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

    private void highlightView(View view) {
        List<Integer> layoutList = Arrays.asList(R.id.standard, R.id.safer, R.id.safest);
        for (int i = 0; i < layoutList.size(); i++) {
            if (view.getId() == layoutList.get(i)) {
                view.setBackground(ContextCompat.getDrawable(this, R.drawable.garlic_level_background_focused));
            } else {
                findViewById(layoutList.get(i))
                        .setBackground(ContextCompat.getDrawable(this, R.drawable.garlic_level_background));
            }
        }
    }
}