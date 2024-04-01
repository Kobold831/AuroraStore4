/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.connection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.aurora.Constants;
import com.aurora.extensions.ToastKt;
import com.aurora.store.R;
import com.aurora.store.data.installer.InstallerService;
import com.aurora.store.data.service.DhizukuService;
import com.aurora.store.data.service.IDhizukuService;
import com.aurora.store.util.Common;
import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

public class Updater {

    IDhizukuService mDhizukuService;
    Activity activity;

    public Updater(Activity act) {
        activity = act;
    }

    public void installApk() {
        switch (Common.GET_UPDATE_MODE(activity)) {
            case 0 -> new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_cpad_title_update)
                    .setMessage(R.string.dialog_cpad_update_caution)
                    .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> {
                        try {
                            activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WIKI_MAIN)), Constants.REQUEST_UPDATE);
                        } catch (ActivityNotFoundException ignored) {
                            ToastKt.toast(activity, R.string.toast_cpad_unknown_activity);
                            activity.finish();
                        }
                    })
                    .show();
            case 1 -> {
                if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
                    try {
                        xInstall();
                    } catch (IOException ignored) {
                        Common.SET_UPDATE_MODE(activity, 0);
                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_error + "\nアップデートモードをリセットしました")
                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                                .show();
                    }
                } else {
                    Common.SET_UPDATE_MODE(activity, 0);
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(activity.getString(R.string.dialog_cpad_error_reset_update_mode))
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                            .show();
                }
            }
            case 2 -> {
                if (tryBindDhizukuService(activity)) {
                    dInstall();
                } else {
                    Common.SET_UPDATE_MODE(activity, 0);
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_cpad_error + "\nアップデートモードをリセットしました")
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                            .show();
                }
            }
        }
    }

    public boolean tryBindDhizukuService(Context context) {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }

    private void xInstall() throws IOException {
        if (!trySessionInstall()) {
            Common.SET_UPDATE_MODE(activity, 0);
            new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setMessage(R.string.dialog_cpad_error + "\nアップデートモードをリセットしました")
                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                    .show();
        }
    }

    private void dInstall() {
        Runnable runnable = () -> {
            try {
                if (!mDhizukuService.tryInstallPackages(Collections.singletonList(new File(activity.getExternalCacheDir(), "update.apk").getPath()))) {
                    Common.SET_UPDATE_MODE(activity, 0);
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_cpad_error + "\nアップデートモードをリセットしました")
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                            .show();
                }
            } catch (RemoteException ignored) {
                Common.SET_UPDATE_MODE(activity, 0);
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setMessage(R.string.dialog_cpad_error + "\nアップデートモードをリセットしました")
                        .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                        .show();
            }
        };
        new Handler().postDelayed(runnable, 5000);
    }

    private boolean trySessionInstall() throws IOException {
        int sessionId = createSession(activity.getPackageManager().getPackageInstaller());
        if (sessionId < 0) {
           return false;
        }
        writeSession(activity.getPackageManager().getPackageInstaller(), sessionId, new File(activity.getExternalCacheDir(), "update.apk"));
        commitSession(activity.getPackageManager().getPackageInstaller(), sessionId, activity);
        return true;
    }

    private int createSession(PackageInstaller packageInstaller) throws IOException {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY);
        return packageInstaller.createSession(params);
    }

    private void writeSession(PackageInstaller packageInstaller, int sessionId, File apkFile) throws IOException {
        long sizeBytes = -1;
        String apkPath = apkFile.getAbsolutePath();

        File file = new File(apkPath);
        if (file.isFile()) {
            sizeBytes = file.length();
        }

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            session = packageInstaller.openSession(sessionId);
            in = new FileInputStream(apkPath);
            out = session.openWrite(getRandomString(), 0, sizeBytes);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
        } finally {
            if (out != null) {
                out.close();
                in.close();
                session.close();
            }
        }
    }

    private void commitSession(PackageInstaller packageInstaller, int sessionId, Context context) {
        try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
            Intent intent = new Intent(context, InstallerService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception ignored) {
        }
    }

    private String getRandomString() {
        String theAlphaNumericS;
        StringBuilder builder;
        theAlphaNumericS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        builder = new StringBuilder(5);
        for (int m = 0; m < 5; m++) {
            int myindex = (int) (theAlphaNumericS.length() * Math.random());
            builder.append(theAlphaNumericS.charAt(myindex));
        }
        return builder.toString();
    }
}