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

package com.aurora.store.data.event;

import java.util.HashSet;
import java.util.Set;

public class DownloadEventListenerList {

	private final Set<DownloadEventListener> listeners = new HashSet<>();

	public void addEventListener(DownloadEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify(int reqCode) {
		for (DownloadEventListener listener : listeners) listener.onDownloadComplete(reqCode);
	}

	public void downloadErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onDownloadError();
	}

	public void connectionErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onConnectionError();
	}
}