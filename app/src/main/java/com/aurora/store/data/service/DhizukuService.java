package com.aurora.store.data.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.preference.PreferenceManager;

import com.aurora.store.data.installer.InstallerService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DhizukuService extends IDhizukuService.Stub {

    private Context context;

    @Keep
    public DhizukuService(Context context) {
        this.context = context;
    }

    @Override
    public boolean tryInstallPackages(String str, List<Uri> uriList) throws RemoteException {
        int sessionId;

        try {
            sessionId = createSession(context, context.getPackageManager().getPackageInstaller());
            if (sessionId < 0) {
                context.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            return false;
        }

        for (Uri uri : uriList) {
            try {
                if (!writeSession(context.getPackageManager().getPackageInstaller(), sessionId, new File(Environment.getExternalStorageDirectory() + uri.getPath().replace("/external_files", "")))) {
                    context.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                    return false;
                }
            } catch (IOException ignored) {
                context.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        }
        try {
            if (commitSession(context.getPackageManager().getPackageInstaller(), sessionId, context)) {
                return true;
            } else {
                context.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            context.getPackageManager().getPackageInstaller().abandonSession(sessionId);
            return false;
        }
    }

    public static int createSession(Context context, PackageInstaller packageInstaller) throws IOException {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pre_owner_install_location", false)) {
            params.setInstallLocation(PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL);
        } else params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY);

        return packageInstaller.createSession(params);
    }

    public static boolean writeSession(PackageInstaller packageInstaller, int sessionId, File apkFile) throws IOException {
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
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    public static boolean commitSession(PackageInstaller packageInstaller, int sessionId, Context context) throws IOException {
        PackageInstaller.Session session = null;

        try {
            session = packageInstaller.openSession(sessionId);
            Intent intent = new Intent(context, InstallerService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );

            session.commit(pendingIntent.getIntentSender());
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    public static String getRandomString() {
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