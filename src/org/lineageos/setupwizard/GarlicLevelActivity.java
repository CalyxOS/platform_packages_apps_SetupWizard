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

import android.os.Bundle;

public class GarlicLevelActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNextAllowed(true);
        getGlifLayout().setDescriptionText(getString(R.string.security_level_subtitle));
    }

    @Override
    protected void onNextPressed() {
        nextAction(RESULT_OK);
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

}