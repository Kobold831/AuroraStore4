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
import android.os.AsyncTask;

import com.aurora.store.data.event.UpdateEventListener;
import com.aurora.store.data.event.UpdateEventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Checker {

    private int supportCode;
    private final String supportCheckUrl;
    private final UpdateEventListenerList supportListeners;

    public Checker(Activity activity, String url) {
        supportCheckUrl = url;
        supportListeners = new UpdateEventListenerList();
        supportListeners.addEventListener((UpdateEventListener) activity);
    }

    private int supportAvailableCheck() {

        try {
            getSupportInfo();
            if (supportCode == 1) {
                return 1;
            } else {
                if (supportCode == 0) {
                    return 0;
                } else return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void getSupportInfo() {

        HashMap<String, String> map = parseSupportXml(supportCheckUrl);

        if (map != null) {
            supportCode = Integer.parseInt(map.get("supportCode"));
        } else {
            supportCode = -99;
        }
    }

    public void supportCheck() {
        new supportCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class supportCheckTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... arg0) {
            return supportAvailableCheck();
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case -1:
                    supportListeners.connectionErrorNotify();
                    break;
                case 0:
                    supportListeners.supportUnavailableNotify();
                    break;
                case 1:
                    supportListeners.supportAvailableNotify();
                    break;
            }
        }
    }

    private HashMap<String, String> parseSupportXml(String url) {

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

            if (root.getTagName().equals("support")) {
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