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
import android.os.AsyncTask;

import com.aurora.store.data.event.DownloadEventListener;
import com.aurora.store.data.event.DownloadEventListenerList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class AsyncFileDownload extends AsyncTask<String, Void, Boolean> {

	DownloadEventListenerList downloadEventListenerList;
	String url;
	int reqCode;
	File outputFile;
	FileOutputStream fileOutputStream;
	BufferedInputStream bufferedInputStream;
	int totalByte = 0, currentByte = 0;

	public AsyncFileDownload(Activity activity, String str, File file, int i) {
		url = str;
		outputFile = file;
		reqCode = i;
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener((DownloadEventListener) activity);
	}

	@Override
	protected Boolean doInBackground(String... str) {
		final byte[] buffer = new byte[1024];

		try {
			HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
			httpURLConnection.setReadTimeout(5000);
			httpURLConnection.setConnectTimeout(5000);
			bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), 1024);
			fileOutputStream = new FileOutputStream(outputFile);
			totalByte = httpURLConnection.getContentLength();
		} catch (SocketTimeoutException | MalformedURLException ignored) {
			return false;
		} catch (IOException ignored) {
			return null;
		}

		if (isCancelled()) {
			return false;
		}

		try {
			int len;

			while ((len = bufferedInputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, len);
				currentByte += len;

				if (isCancelled()) break;
			}
		} catch (IOException ignored) {
			return false;
		}

		try {
			close();
		} catch (IOException ignored) {
		}

		return true;
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result != null) {
			if (result) downloadEventListenerList.downloadCompleteNotify(reqCode);
			else downloadEventListenerList.downloadErrorNotify();
		} else downloadEventListenerList.connectionErrorNotify();
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
	}

	private void close() throws IOException {
		fileOutputStream.flush();
		fileOutputStream.close();
		bufferedInputStream.close();
	}

	public int getLoadedBytePercent() {
		if (totalByte <= 0) return 0;
		return (int) Math.floor((double) (100 * currentByte) / totalByte);
	}
}