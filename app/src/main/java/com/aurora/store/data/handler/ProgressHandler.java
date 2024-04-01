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

package com.aurora.store.data.handler;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.aurora.store.data.connection.AsyncFileDownload;

public class ProgressHandler extends Handler {

    public ProgressDialog progressDialog;
    public AsyncFileDownload asyncfiledownload;

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (asyncfiledownload.isCancelled()) progressDialog.dismiss();
        else if (asyncfiledownload.getStatus() == AsyncTask.Status.FINISHED)
            progressDialog.dismiss();
        else {
            progressDialog.setProgress(asyncfiledownload.getLoadedBytePercent());
            sendEmptyMessageDelayed(0, 100);
        }
    }
}