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

package com.aurora.store.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.aurora.store.R;
import com.aurora.store.data.event.InstallerEvent;

import org.greenrobot.eventbus.EventBus;

public class ResultService extends Service {

    protected IInstallResult.Stub iInstallResultStub = new IInstallResult.Stub() {
        @Override
        public void InstallSuccess(String packageName) {
            EventBus.getDefault().post(new InstallerEvent.Success(packageName, getString(R.string.installer_status_success)));
        }

        @Override
        public void InstallFailure(String packageName, String errorString) {
            EventBus.getDefault().post(new InstallerEvent.Cancelled(packageName, errorString));
        }

        @Override
        public void InstallError(String packageName, String errorString, String extra) {
            EventBus.getDefault().post(new InstallerEvent.Failed(packageName, errorString, extra));
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return iInstallResultStub;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}