/*
 * Niconicohelper
 *
 * Copyright (c) saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

package com.serenegiant.nicolivehelper;

import android.util.Log;

import com.android.volley.Response;

import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XmlRequest extends InputStreamRequest {
	private static final String TAG = XmlRequest.class.getSimpleName();

	public interface XmlResponseListener {
		public void onResponse(final Document response);
	}

	public XmlRequest(final int method, final String url, Map<String, String> params,
		final XmlResponseListener listener, final Response.ErrorListener error_listener) {

		super(method, url, params, new Response.Listener<InputStream>() {
			@Override
			public void onResponse(final InputStream response) {
				if (listener != null) {
					final BufferedInputStream bufferedInputStream = new BufferedInputStream(response);
					try {
						final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						final DocumentBuilder builder = factory.newDocumentBuilder();
						final Document document = builder.parse(bufferedInputStream);
						listener.onResponse(document);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
		}, error_listener);
	}

}
