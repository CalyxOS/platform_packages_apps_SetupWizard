/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.wizardmanager;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.util.Objects;

public class WizardTransitions extends SparseArray<String> implements Parcelable {

    private static final String TAG = "WizardTransitions";

    private String mDefaultAction;

    public static final Creator<WizardTransitions> CREATOR = new Creator<>() {
        public WizardTransitions createFromParcel(Parcel source) {
            WizardTransitions transitions = new WizardTransitions(source);
            SparseArray<String> actions = source.readSparseArray(null, String.class);
            for (int i = 0; i < actions.size(); i++) {
                transitions.put(actions.keyAt(i), actions.valueAt(i));
            }
            return transitions;
        }

        public WizardTransitions[] newArray(int size) {
            return new WizardTransitions[size];
        }
    };

    public WizardTransitions() {
    }

    public void setDefaultAction(String action) {
        mDefaultAction = action;
    }

    public String getAction(int resultCode) {
        return get(resultCode, mDefaultAction);
    }

    @Override
    public void put(int key, String value) {
        if (LOGV) {
            Log.v(TAG, "put{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    '}');
        }
        super.put(key, value);
    }

    @NonNull
    public String toString() {
        return super.toString() + " mDefaultAction: " + mDefaultAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WizardTransitions that = (WizardTransitions) o;
        return Objects.equals(mDefaultAction, that.mDefaultAction);

    }

    public int hashCode() {
        return super.hashCode() + mDefaultAction.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDefaultAction);
        int size = size();
        SparseArray<String> sparseArray = new SparseArray<>(size);
        for (int i = 0; i < size; i++) {
            sparseArray.put(keyAt(i), valueAt(i));
        }
        dest.writeSparseArray(sparseArray);
    }

    protected WizardTransitions(Parcel in) {
        mDefaultAction = in.readString();
    }

}
