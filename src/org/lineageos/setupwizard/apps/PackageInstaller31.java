/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2019-2021 The Calyx Institute
 *
 *  Based on code from com.android.packageinstaller.InstallInstalling
 *  frameworks/base/packages/PackageInstaller/src/com/android/packageinstaller/InstallInstalling.java
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

import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;
import static android.content.pm.PackageInstaller.SessionParams;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.content.pm.PackageManager.INSTALL_FULL_APP;
import static android.content.pm.PackageParser.PackageParserException;
import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;

import android.annotation.WorkerThread;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageManager;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.content.InstallLocationUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

class PackageInstaller31 {

    private static final String TAG = PackageInstaller31.class.getSimpleName();
    private static final String PRIV_EXT_PACKAGE_ID = "org.fdroid.fdroid.privileged";
    private static final String BROADCAST_ACTION =
            "com.android.packageinstaller.ACTION_INSTALL_COMMIT";
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final Context context;
    private final PackageManager pm;
    private final PackageInstaller installer;

    PackageInstaller31(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.installer = pm.getPackageInstaller();
    }

    @WorkerThread
    void install(File packageFile) throws IOException, SecurityException {
        install(packageFile, PRIV_EXT_PACKAGE_ID);
    }

    @WorkerThread
    void install(File packageFile, String installerPackageName) throws IOException,
            SecurityException {
        if (!packageFile.isFile()) throw new IOException("Cannot read package file " + packageFile);

        SessionParams params = new SessionParams(MODE_FULL_INSTALL);
        params.installFlags = INSTALL_FULL_APP;
        params.installerPackageName = installerPackageName;
        params.installReason = PackageManager.INSTALL_REASON_DEVICE_SETUP;

        try {
            final ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            final ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(
                    input.reset(), packageFile, /* flags */ 0);
            if (result.isError()) {
                Log.e(TAG, "Cannot parse package " + packageFile + ". Assuming defaults.");
                Log.e(TAG, "Cannot calculate installed size " + packageFile +
                        ". Try only apk size.");
                params.setSize(packageFile.length());
            } else {
                final PackageLite pkg = result.getResult();
                params.setAppPackageName(pkg.getPackageName());
                params.setInstallLocation(pkg.getInstallLocation());
                params.setSize(InstallLocationUtils.calculateInstalledSize(pkg, params.abiOverride));
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot calculate installed size " + packageFile + ". Try only apk size.");
            params.setSize(packageFile.length());
        }

        int sessionId = installer.createSession(params);
        Session session = installer.openSession(sessionId);
        try {
            writePackage(packageFile, session);
        } catch (IOException e) {
            Log.e(TAG, "Error installing package " + packageFile);
            session.close();
            throw e;
        }
        session.commit(getIntentSender());
    }

    private void writePackage(File packageFile, Session session) throws IOException {
        try (InputStream in = new FileInputStream(packageFile)) {
            long sizeBytes = packageFile.length();
            try (OutputStream out = session.openWrite("PackageInstaller", 0, sizeBytes)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    int numRead = in.read(buffer);
                    if (numRead == -1) {
                        session.fsync(out);
                        break;
                    }
                    out.write(buffer, 0, numRead);
                }
            }
        }
    }

    private IntentSender getIntentSender() {
        Intent broadcastIntent = new Intent(BROADCAST_ACTION);
        broadcastIntent.setFlags(FLAG_RECEIVER_FOREGROUND);
        broadcastIntent.setPackage(pm.getPermissionControllerPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent,
                FLAG_UPDATE_CURRENT | FLAG_MUTABLE);
        return pendingIntent.getIntentSender();
    }

}
