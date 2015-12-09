/*
 *
 * Niconicohelper
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
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
 *
 */

package com.serenegiant.nicolivehelper;

import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class InputStreamRequest extends Request<InputStream> {
	/** デフォルトの文字セット=UTF8 */
	protected static final String INPUT_CHARSET = "utf-8";

	/** コンテントタイプ */
	private static final String INPUT_CONTENT_TYPE =
    	String.format("application/x-www-form-urlencoded; charset=%s", INPUT_CHARSET);

	public static final String KEY_USER_AGENT = "User-Agent";
	public static final String KEY_CONTENT_TYPE = "Content-Type";

	private final Response.Listener<InputStream> mListener;
	private final Map<String, String> mParams = new HashMap<String, String>();

	public InputStreamRequest(final int method, final String url, final Map<String, String> params,
		final Response.Listener<InputStream> listener,
		final Response.ErrorListener error_listener) {

		super(method, url, error_listener);
		mListener = listener;
		mParams.put(KEY_CONTENT_TYPE, INPUT_CONTENT_TYPE);
		addParams(params);
	}

	@Override
	protected Response<InputStream> parseNetworkResponse(NetworkResponse response) {
		final InputStream is = new ByteArrayInputStream(response.data);
		return Response.success(is, HttpHeaderParser.parseCacheHeaders(response));
	}

	@Override
	protected void deliverResponse(final InputStream response) {
		mListener.onResponse(response);
	}

	@Override
	public String getBodyContentType() {
		return INPUT_CONTENT_TYPE;
	}

	@Override
	public byte[] getBody() {
		final StringBuilder encodedParams = new StringBuilder();
		try {
			for (final Map.Entry<String, String> entry : mParams.entrySet()) {
				encodedParams.append(URLEncoder.encode(entry.getKey(), INPUT_CHARSET));
				encodedParams.append('=');
				encodedParams.append(URLEncoder.encode(entry.getValue(), INPUT_CHARSET));
				encodedParams.append('&');
			}
			final String params = encodedParams.toString();
			return TextUtils.isEmpty(params) ? null : params.getBytes(INPUT_CHARSET);
		} catch (final UnsupportedEncodingException uee) {
			VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mParams.toString(), INPUT_CHARSET);
			return null;
		}
	}

	public void addParams(final Map<String, String> params) {
		if (params == null) return;
		mParams.putAll(params);
	}
}
