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

import android.annotation.SuppressLint;
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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.aurora.Constants;
import com.aurora.extensions.ToastKt;
import com.aurora.store.R;
import com.aurora.store.data.event.UpdateEventListener;
import com.aurora.store.data.event.UpdateEventListenerList;
import com.aurora.store.data.installer.InstallerService;
import com.aurora.store.util.Common;
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Updater {

    IDeviceOwnerService mDeviceOwnerService;
    private final UpdateEventListenerList updateListeners;
    private final Activity activity;

    public Updater(Activity mActivity) {
        activity = mActivity;
        updateListeners = new UpdateEventListenerList();
        updateListeners.addEventListener((UpdateEventListener) activity);
    }

    public static class Result {
        public final int versionCode;
        public final String msg;

        public Result(int versionCode, String msg) {
            this.versionCode = versionCode;
            this.msg = msg;
        }
    }

    private int getCurrentVersionInfo() throws Exception {
        return activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_META_DATA).versionCode;
    }

    private Result getLatestVersionInfo() {
        HashMap<String, String> map = parseUpdateXml(Constants.CPAD_UPDATE_CHECK_URL);
        if (map != null) {
            Common.DOWNLOAD_FILE_URL = map.get("url");
            return new Result(Integer.parseInt(map.get("versionCode")), map.get("description"));
        } else return new Result(-99, null);
    }

    public void updateCheck() {
        new updateCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class updateCheckTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... arg0) {
            try {
                if (getLatestVersionInfo().versionCode == -99) return -1;
                if (getCurrentVersionInfo() < getLatestVersionInfo().versionCode)
                    return getLatestVersionInfo().msg;
                else return 0;
            } catch (Exception ignored) {
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result.equals(-1)) updateListeners.connectionErrorNotify();
            if (result.equals(0)) updateListeners.updateUnavailableNotify();
            else updateListeners.updateAvailableNotify((String) result);
        }
    }

    public void installApk() {
        switch (Common.GET_UPDATE_MODE(activity)) {
            case 0:
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_cpad_title_update)
                        .setMessage(R.string.dialog_cpad_update_caution)
                        .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> {
                            try {
                                activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CPAD_WIKI_URL)), Constants.CPAD_REQUEST_UPDATE);
                            } catch (ActivityNotFoundException ignored) {
                                ToastKt.toast(activity, R.string.toast_cpad_unknown_activity);
                                activity.finish();
                            }
                        })
                        .show();
                break;
            case 1:
                if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
                    try {
                        xInstall();
                    } catch (IOException ignored) {
                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_error)
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
                break;
            case 2:
                if (bindDeviceOwnerService()) {
                    oInstall();
                } else {
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_cpad_error)
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                            .show();
                }
                break;
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDeviceOwnerService = null;
        }
    };

    public boolean bindDeviceOwnerService() {
        try {
            return activity.bindService(Common.BIND_CUSTOMIZE_TOOL, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void xInstall() throws IOException {
        if (!trySessionInstall()) {
            new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setMessage(R.string.dialog_cpad_error)
                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                    .show();
        }
    }

    private void oInstall() {
        Runnable runnable = () -> {
            try {
                if (!mDeviceOwnerService.installPackages("", Collections.singletonList(Uri.parse(Uri.fromFile(new File("/external_files/Android/data/com.aurora.store/cache/update.apk")).getPath())))) {
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_cpad_error)
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                            .show();
                }
            } catch (RemoteException ignored) {
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setMessage(R.string.dialog_cpad_error)
                        .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> activity.finishAffinity())
                        .show();
            }
        };
        new Handler().postDelayed(runnable, 1000);
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
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);
            Intent intent = new Intent(context, InstallerService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception ignored) {
        } finally {
            if (session != null) {
                session.close();
            }
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

    private HashMap<String, String> parseUpdateXml(String url) {

        HashMap<String, String> map = new HashMap<>();
        HttpURLConnection mHttpURLConnection;

        try {
            mHttpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            mHttpURLConnection.setConnectTimeout(5000);
            InputStream is = mHttpURLConnection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
            Document document = document_builder.parse(bis);
            Element root = document.getDocumentElement();

            if (root.getTagName().equals("update")) {
                NodeList nodelist = root.getChildNodes();
                for (int j = 0; j < nodelist.getLength(); j++) {
                    Node node = nodelist.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getTagName();
                        String value = element.getTextContent().trim();
                        map.put(name, value);
                    }
                }
            }
            return map;
        } catch (SocketTimeoutException | MalformedURLException ignored) {
            return null;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }
}