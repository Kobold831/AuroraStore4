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

import com.aurora.store.data.event.UpdateEventListener;
import com.aurora.store.data.event.UpdateEventListenerList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class AsyncFileDownload extends AsyncTask<String, Void, Boolean> {

	UpdateEventListenerList updateListeners;
	String fileUrl;
	File outputFile;
	FileOutputStream fileOutputStream;
	BufferedInputStream bufferedInputStream;
	int totalByte = 0, currentByte = 0;

	public AsyncFileDownload(Activity activity, String url, File oFile) {
		updateListeners = new UpdateEventListenerList();
		updateListeners.addEventListener((UpdateEventListener) activity);
		fileUrl = url;
		outputFile = oFile;
	}

	@Override
	protected Boolean doInBackground(String... mString) {
		final byte[] buffer = new byte[1024];

		try {
			HttpURLConnection mHttpURLConnection;
			mHttpURLConnection = (HttpURLConnection) new URL(fileUrl).openConnection();
			mHttpURLConnection.setReadTimeout(5000);
			mHttpURLConnection.setConnectTimeout(5000);
			InputStream mInputStream = mHttpURLConnection.getInputStream();
			bufferedInputStream = new BufferedInputStream(mInputStream, 1024);
			fileOutputStream = new FileOutputStream(outputFile);
			totalByte = mHttpURLConnection.getContentLength();
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
			if (result) updateListeners.downloadCompleteNotify();
			else updateListeners.connectionErrorNotify();
		} else updateListeners.downloadErrorNotify();
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
		return (int) Math.floor(100 * currentByte / totalByte);
	}
}