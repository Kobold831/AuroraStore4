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

public class UpdateEventListenerList {

	private final Set<UpdateEventListener> listeners = new HashSet<>();

	public void addEventListener(UpdateEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify() {
		for (UpdateEventListener listener : listeners) listener.onUpdateApkDownloadComplete();
	}

	public void updateAvailableNotify(String mString) {
		for (UpdateEventListener listener : listeners) listener.onUpdateAvailable(mString);
	}

	public void updateUnavailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onUpdateUnavailable();
	}

	public void supportAvailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onSupportAvailable();
	}

	public void supportUnavailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onSupportUnavailable();
	}

	public void connectionErrorNotify() {
		for (UpdateEventListener listener : listeners) listener.onConnectionError();
	}

	public void downloadErrorNotify() {
		for (UpdateEventListener listener : listeners) listener.onDownloadError();
	}
}