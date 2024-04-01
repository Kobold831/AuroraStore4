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

package com.aurora.store.view.ui.launch;

import static com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID;

import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.Constants;
import com.aurora.extensions.ToastKt;
import com.aurora.store.BuildConfig;
import com.aurora.store.MainActivity;
import com.aurora.store.R;
import com.aurora.store.data.connection.AsyncFileDownload;
import com.aurora.store.data.connection.Updater;
import com.aurora.store.data.event.DownloadEventListener;
import com.aurora.store.data.handler.ProgressHandler;
import com.aurora.store.util.Common;
import com.aurora.store.util.PreferencesKt;
import com.aurora.store.util.Variables;
import com.aurora.store.view.epoxy.views.UpdateModeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StartActivity extends AppCompatActivity implements DownloadEventListener {

    DevicePolicyManager dpm;
    ProgressDialog loadingDialog;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        /* ネットワークチェック */
        if (!isNetworkState()) {
            networkError();
        } else updateCheck();
    }

    /* ネットワークの接続を確認 */
    private boolean isNetworkState() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void networkError() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_wifi)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void updateCheck() {
        showLoadingDialog();
        new AsyncFileDownload(this, Constants.URL_CHECK, new File(new File(getExternalCacheDir(), "Check.json").getPath()), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK).execute();
    }

    private void supportCheck() {
        showLoadingDialog();
        new AsyncFileDownload(this, Constants.URL_CHECK, new File(new File(getExternalCacheDir(), "Check.json").getPath()), Constants.REQUEST_DOWNLOAD_SUPPORT_CHECK).execute();
    }

    public JSONObject parseJson() throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(getExternalCacheDir(), "Check.json").getPath()));
        JSONObject json;

        StringBuilder data = new StringBuilder();
        String str = bufferedReader.readLine();

        while (str != null) {
            data.append(str);
            str = bufferedReader.readLine();
        }

        json = new JSONObject(data.toString());

        bufferedReader.close();

        return json;
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK -> {
                try {
                    JSONObject jsonObj1 = parseJson();
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("as");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");
                    Variables.DOWNLOAD_FILE_URL = jsonObj3.getString("url");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        cancelLoadingDialog();
                        showUpdateDialog(jsonObj3.getString("description"));
                    } else {
                        cancelLoadingDialog();
                        supportCheck();
                    }
                } catch (JSONException | IOException ignored) {
                }
            }
            case Constants.REQUEST_DOWNLOAD_SUPPORT_CHECK -> {
                try {
                    JSONObject jsonObj1 = parseJson();
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("as");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("support");

                    if (jsonObj3.getInt("supportCode") == 0) {
                        cancelLoadingDialog();
                        /* サポートモデルか確認 */
                        if (supportModelCheck()) {
                            setInstaller();
                        } else {
                            supportModelError();
                        }
                    } else {
                        cancelLoadingDialog();
                        showSupportDialog();
                    }
                } catch (JSONException | IOException ignored) {
                }
            }
            case Constants.REQUEST_DOWNLOAD_APK -> new Handler().post(() -> new Updater(this).installApk());
        }
    }

    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage("ダウンロードに失敗しました\nネットワークが安定しているか確認してください")
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_connection)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    public void setInstaller() {
        /* AuroraStoreがデバイスオーナーならXInstallerへ、デバイスオーナーではないならバインド試行 */
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            XInstaller();
            return;
        }

        try {
            if (Common.hasShizukuOrSui(this)) {
                if (Common.hasShizukuPerm()) {
                    SInstaller();
                    return;
                }
            }
        } catch (Exception e) {
            DInstaller();
            return;
        }

        DInstaller();
    }

    /* デバイスオーナーインストーラー設定 */
    public void XInstaller() {
        PreferencesKt.save(this, PREFERENCE_INSTALLER_ID, 0);

        if (Common.GET_SETTINGS_FLAG(this) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
            WarningDialog();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /* Shizukuインストーラー設定 */
    public void SInstaller() {
        PreferencesKt.save(this, PREFERENCE_INSTALLER_ID, 1);

        if (Common.GET_SETTINGS_FLAG(this) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
            WarningDialog();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /* Dhizukuインストーラー設定 */
    public void DInstaller() {
        /* バインド失敗した場合はDhizuku通信試行 */
        /* Dhizuku通信と権限許可プロンプト処理実装予定 */
        if (!Dhizuku.init(this)) {
            /* Dhizukuが失敗した場合はエラー */
            new MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_cpad_title_start_error)
                    .setMessage(R.string.dialog_cpad_error_installer)
                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .show();
            return;
        }

        if (!Dhizuku.isPermissionGranted()) {
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    runOnUiThread(() -> {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            PreferencesKt.save(getApplicationContext(), PREFERENCE_INSTALLER_ID, 2);

                            if (Common.GET_SETTINGS_FLAG(getApplicationContext()) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
                                WarningDialog();
                            } else {
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            }
                        } else {
                            /* Dhizukuが失敗した場合はエラー */
                            new MaterialAlertDialogBuilder(StartActivity.this)
                                    .setCancelable(false)
                                    .setTitle(R.string.dialog_cpad_title_start_error)
                                    .setMessage(R.string.dialog_cpad_error_installer)
                                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> finishAndRemoveTask())
                                    .show();
                        }
                    });
                }
            });
        } else {
            PreferencesKt.save(this, PREFERENCE_INSTALLER_ID, 2);

            if (Common.GET_SETTINGS_FLAG(this) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
                WarningDialog();
            } else {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
    }

    private void showUpdateDialog(String str) {
        View view = getLayoutInflater().inflate(R.layout.sheet_cpad_update, null);
        TextView tv = view.findViewById(R.id.cpad_update_info);
        tv.setText(str);
        view.findViewById(R.id.button_cpad_update_info).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE_INFO)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                ToastKt.toast(this, R.string.toast_cpad_unknown_activity);
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cpad_title_update)
                .setMessage("アップデートモードを変更するには”設定”を押下してください")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()), Constants.REQUEST_DOWNLOAD_APK);
                    asyncFileDownload.execute();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_cpad_title_update);
                    progressDialog.setMessage(getString(R.string.progress_cpad_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cpad_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        finishAffinity();
                    });
                    if (!progressDialog.isShowing()) progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog, which) -> finishAffinity())
                .setNeutralButton(R.string.dialog_cpad_title_settings, (dialog, which) -> {
                    dialog.dismiss();
                    showUpdateMode(str);
                })
                .show();
    }

    private void showUpdateMode(String s) {
        View v = getLayoutInflater().inflate(R.layout.layout_cpad_update_list, null);
        List<UpdateModeView.AppData> dataList = new ArrayList<>();
        int i = 0;
        for (String str : Common.list) {
            UpdateModeView.AppData data = new UpdateModeView.AppData();
            data.label = str;
            data.updateMode = i;
            dataList.add(data);
            i++;
        }
        ListView listView = v.findViewById(R.id.list_cpad_update);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new UpdateModeView.AppListAdapter(this, dataList));
        listView.setOnItemClickListener((parent, mView, position, id) -> {
            switch (position) {
                case 0 -> {
                    Common.SET_UPDATE_MODE(this, (int) id);
                    listView.invalidateViews();
                }
                case 1 -> {
                    if (((DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(getPackageName())) {
                        Common.SET_UPDATE_MODE(this, (int) id);
                        listView.invalidateViews();
                    } else {
                        new MaterialAlertDialogBuilder(this)
                                .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
                case 2 -> {
                    if (!Dhizuku.init(this)) {
                        new MaterialAlertDialogBuilder(this)
                                .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                        return;
                    }

                    if (!Dhizuku.isPermissionGranted()) {
                        Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                            @Override
                            public void onRequestPermission(int grantResult) {
                                runOnUiThread(() -> {
                                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                        Common.SET_UPDATE_MODE(StartActivity.this, (int) id);
                                        listView.invalidateViews();
                                        return;
                                    } else {
                                        new MaterialAlertDialogBuilder(StartActivity.this)
                                                .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                                .show();
                                        return;
                                    }
                                });
                            }
                        });
                    } else {
                        if (Common.isDhizukuActive(this)) {
                            Common.SET_UPDATE_MODE(this, (int) id);
                            listView.invalidateViews();
                        } else {
                            new MaterialAlertDialogBuilder(this)
                                    .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    }
                }
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .setTitle(getString(R.string.dialog_cpad_title_select_mode))
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    dialog.dismiss();
                    showUpdateDialog(s);
                })
                .show();
    }

    private void showSupportDialog() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_not_use)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_cpad_state_connection), true);
        loadingDialog.show();
    }

    private void cancelLoadingDialog() {
        try {
            if (loadingDialog != null) loadingDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    public boolean supportModelCheck() {
        String[] modelName = {"TAB-A05-BD", "TAB-A05-BA1"};
        for (String string : modelName) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
        return false;
    }

    /* 端末チェックエラー */
    private void supportModelError() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_not_neo)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    /* 初回起動お知らせ */
    public void WarningDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_notice_start)
                .setMessage(R.string.dialog_cpad_notice_start)
                .setPositiveButton(R.string.dialog_cpad_agree, (dialog, which) -> {
                    Common.SET_SETTINGS_FLAG(Constants.CPAD_SETTINGS_COMPLETED, this);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.dialog_cpad_disagree, (dialog, which) -> {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dpm.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .create();
        alertDialog.show();
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_UPDATE) {
            finishAndRemoveTask();
        }
    }
}