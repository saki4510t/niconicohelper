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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * ニコニコ動画のライブ配信用URLを取得するためのヘルパークラス
 */
public class NicoliveHelper {
	private static final boolean DEBUG = true;	// FIXME 実同時はfalseにすること
	private static final String TAG = "NicoliveHelper";

	public static final String URL_POST_LOGIN = "https://account.nicovideo.jp/api/v1/login";
	public static final String URL_POST_NLE_UPDATE = "http://live.nicovideo.jp/encoder/update.xml";
	public static final String URL_POST_NLE_GTE_LICENCE = "http://live.nicovideo.jp/encoder/getlicence";
	public static final String URL_POST_PUBLISH_GET_STATUS = "http://live.nicovideo.jp/api/getpublishstatus";

	public static final String USER_AGENT = "NicoliveHelper/0.0.1";

	public interface Callback {
		/** ログインできた時の処理 */
		public void onLogin(final NicoliveHelper helper);
		/** ライブ配信情報を取得できた時の処理 */
		public void onUpdatePublishInfo(final NicoliveHelper helper, final List<PublishInfo> info);
		/** ログイン/配信情報要求/配信情報解析中にエラーが発生した時の処理 */
		public void onError(final NicoliveHelper helper, final Exception e);
	}

	private static RequestQueue sQueue;
	private String mTicket;
	private Callback mCallback;

	/**
	 * コンストラクタ
	 * @param context nullじゃだめよ
	 * @param callback nullじゃだめよ
	 */
	public NicoliveHelper(@NonNull final Context context, @NonNull final Callback callback) {
		super();
		if (sQueue == null) {
			sQueue = Volley.newRequestQueue(context.getApplicationContext());
		}
		mCallback = callback;
//		if (DEBUG) runTest();
	}

	public void release() {
		logout();
	}

	private static final String LOGIN_SITE = "nicolive_encoder";
	private static final String KEY_LOGIN_SITE = "site";
	private static final String KEY_LOGIN_TIME = "time";
	private static final String KEY_LOGIN_HASH_KEY = "hash_key";
	private static final String KEY_LOGIN_MAIL = "mail";
	private static final String KEY_LOGIN_PASSWORD = "password";
	private static final String KEY_STATUS = "status";
	private static final String KEY_TICKET = "ticket";
	private static final String KEY_NLE_SERIAL = "nleserial";
	private static final String KEY_PUBLISH_ACCEPT_MULTI = "accept-multi";

	/**
	 * ログイン開始処理
	 * @param mail
	 * @param password
	 */
	public void login(final String mail, final String password) {
		if (TextUtils.isEmpty(mTicket)) {
			final Map<String, String> params = new HashMap<String, String>();
			params.put(XmlRequest.KEY_USER_AGENT, USER_AGENT);
			params.put(KEY_LOGIN_SITE, LOGIN_SITE);
			params.put(KEY_LOGIN_TIME, Long.toString(System.currentTimeMillis()));
			params.put(KEY_LOGIN_HASH_KEY, null);
			params.put(KEY_LOGIN_MAIL, mail);
			params.put(KEY_LOGIN_PASSWORD, password);
			sQueue.add(new XmlRequest(Request.Method.POST, URL_POST_LOGIN,
				params,
				new XmlRequest.XmlResponseListener() {
					@Override
					public void onResponse(final Document response) {
						parseLoginResponse(response);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(final VolleyError e) {
						// エラー処理 error.networkResponseで確認
						mCallback.onError(NicoliveHelper.this, new RuntimeException("error occurred when login:", e));
					}
				}
			));
		} else {
			// 既にログインしている時
			mCallback.onLogin(this);
		}
	}

	/**
	 * ログイン結果の解析処理
	 * @param response
	 */
	private void parseLoginResponse(final Document response) {
		Log.v(TAG, "parseLoginResponse:");
		final XPathFactory factory = XPathFactory.newInstance();
		final XPath xpath = factory.newXPath();
		try {
			// nicovideo_user_responseノードのstatus属性値を取得する
			final String status = xpath.evaluate("/nicovideo_user_response/@status", response);
			Log.i(TAG, "status=" + status);
			if ("ok".equalsIgnoreCase(status)) {
				// statusがokな時は/nicovideo_user_response/ticketノードの値を文字列として取得する
				final String ticket = xpath.evaluate("/nicovideo_user_response/ticket/text()", response);
				Log.i(TAG, "ticket=" + ticket);
				mTicket = ticket;
				mCallback.onLogin(this);
			} else {
				// statusがokじゃない時
				mCallback.onError(this, new RuntimeException("login failed:status=" + status));
			}
		} catch (final XPathExpressionException e) {
			mCallback.onError(this, new RuntimeException("login failed:", e));
		}
	}

	/**
	 * ログアウトする
	 */
	public void logout() {
		mTicket = null;
	}

	/**
	 * 配信情報を取得する
	 * @param multiple
	 */
	public void requestPublishStatus(final boolean multiple) {
		if (!TextUtils.isEmpty(mTicket)) {
			final Map<String, String> params = new HashMap<String, String>();
			params.put(XmlRequest.KEY_USER_AGENT, USER_AGENT);
			params.put(KEY_TICKET, mTicket);
			params.put(KEY_NLE_SERIAL, null);
			params.put(KEY_PUBLISH_ACCEPT_MULTI, Integer.toString(multiple ? 1 : 0));
			sQueue.add(new XmlRequest(Request.Method.POST, URL_POST_PUBLISH_GET_STATUS,
				params,
				new XmlRequest.XmlResponseListener() {
					@Override
					public void onResponse(final Document response) {
						parsePublishStatus(response, multiple);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(final VolleyError e) {
						// エラー処理 error.networkResponseで確認
						mCallback.onError(NicoliveHelper.this, new RuntimeException("error occurred when requesting publish status:", e));
					}
				}
			));
		} else {
			// loginしていない時
			mCallback.onError(this, new RuntimeException("not login yet"));
		}
	}

	/**
	 * 配信情報を受信した時の解析処理
	 * @param response
	 * @param multiple
	 */
	private void parsePublishStatus(final Document response, final boolean multiple) {
		Log.v(TAG, "parseLoginResponse:");
		final XPathFactory factory = XPathFactory.newInstance();
		final XPath xpath = factory.newXPath();
		final List<PublishInfo> statusList = new ArrayList<PublishInfo>();
		try {
			// nicovideo_user_responseノードのstatus属性値を取得する
			final String status = xpath.evaluate("/getpublishstatus/@status", response);
			Log.i(TAG, "status=" + status);
			if ("ok".equalsIgnoreCase(status)) {
				// statusがokな時は/getpublishstatusノード以下の値を取得する
				final long time = Long.parseLong(xpath.evaluate("/getpublishstatus/@time", response));
				final boolean is_multi = (Boolean)xpath.evaluate("/getpublishstatus/@multi", response, XPathConstants.BOOLEAN);

				final NodeList list = (NodeList)xpath.evaluate("/getpublishstatus/list/item", response, XPathConstants.NODESET);
				final int n = list != null ? list.getLength() : 0;
				if (n > 0) {
					if (DEBUG) Log.v(TAG, "parsePublishStatus:マルチな時");
					if (DEBUG) Log.v(TAG, "parsePublishStatus:マルチな時:n=" + n);
					for (int i = 0; i < n; i++) {
						final PublishInfo item = getItem(xpath, list.item(i));
						if (item != null) {
							statusList.add(item);
						}
					}
				} else {
					if (DEBUG) Log.v(TAG, "parsePublishStatus:マルチなじゃない時");
					final Node node = (Node)xpath.evaluate("/getpublishstatus", response, XPathConstants.NODE);
					final PublishInfo item = getItem(xpath, node);
					if (item != null) {
						statusList.add(item);
					}
				}
				if (DEBUG) dumpPublishStatus(statusList);
				mCallback.onUpdatePublishInfo(this, statusList);
			} else {
				// statusがOKじゃない時
				mCallback.onError(this, new RuntimeException("unexpected status returned:" + status));
			}
		} catch (final XPathExpressionException e) {
			// xmlの解析中にエラーになってもうた時
			mCallback.onError(this, new RuntimeException("parsing publish status failed:" + e.getMessage()));
		}
	}

	/**
	 * 配信情報1つ分を解析して成功すればPublishStatusを生成して返す, 失敗すればnull
	 * @param xpath
	 * @param node
	 * @return
	 * @throws XPathExpressionException
	 */
	private @Nullable PublishInfo getItem(final XPath xpath, final Node node) throws XPathExpressionException {
		if (DEBUG) Log.v(TAG, "getItem:" + node);
		final String info = xpath.evaluate("./stream/text()", node);
		final String url = xpath.evaluate("./rtmp/url/text()", node);
		final String stream = xpath.evaluate("./rtmp/stream/text()", node);
		final String ticket = xpath.evaluate("./rtmp/ticket/text()", node);
		final String bitrate = xpath.evaluate("./rtmp/bitrate/text()", node);
		if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(stream) && !TextUtils.isEmpty(ticket)) {
			final PublishInfo result = new PublishInfo();
			result.title = info;
			result.url = url + "?" + ticket;
			result.stream = stream;
			try {
				result.bitrate = Integer.parseInt(bitrate);
				return result;
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
		return null;
	}

	/**
	 * デバッグ用に配信情報をLogCatに出力する
	 * @param status
	 */
	private void dumpPublishStatus(final List<PublishInfo> status) {
		if (DEBUG) Log.v(TAG, "dumpPublishStatus:");
		final int n = status != null ? status.size() : 0;
		for (int i = 0; i < n; i++) {
			final PublishInfo item = status.get(i);
			Log.v(TAG, item.toString());
		}
	}

	// FIXME このコードはandroidTestへ移したほうがええな
	private void runTest() {
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document login = builder.parse(new InputSource(new StringReader(
				"<nicovideo_user_response status=\"ok\">" +
					" <ticket>nicolive_encoder_1234567890</ticket>" +
					"</nicovideo_user_response>"
			)));
			parseLoginResponse(login);
			final Document publish = builder.parse(new InputSource(new StringReader(
				"<getpublishstatus status=\"ok\" time=\"1432436641\" multi=\"false\">" +
					"<stream>{動画の情報 省略}</stream>" +
					"<user>{ユーザーの情報 省略}</user>" +
					"<rtmp is_fms=\"1\">" +
						"<url>rtmp://{ホスト}.live.nicovideo.jp:{ポート番号}/publicorigin/{数字}</url>" +
						"<stream>lv{数字}</stream>" +
						"<ticket>{ユーザID}:{ライブID}:{数字}:{数字}:{数字}:{数字}:{16進数}</ticket>" +
						"<bitrate>384000</bitrate>" +
					"</rtmp>" +
				"</getpublishstatus>"
			)));
			parsePublishStatus(publish, false);
			final Document publish_fail = builder.parse(new InputSource(new StringReader(
				"<getpublishstatus status=\"fail\" time=\"1432436641\" multi=\"false\">" +
					"<stream>{動画の情報 省略}</stream>" +
					"<user>{ユーザーの情報 省略}</user>" +
					"<rtmp is_fms=\"1\">" +
						"<url>rtmp://{ホスト}.live.nicovideo.jp:{ポート番号}/publicorigin/{数字}</url>" +
						"<stream>lv{数字}</stream>" +
						"<ticket>{ユーザID}:{ライブID}:{数字}:{数字}:{数字}:{数字}:{16進数}</ticket>" +
						"<bitrate>384000</bitrate>" +
					"</rtmp>" +
				"</getpublishstatus>"
			)));
			parsePublishStatus(publish_fail, false);
			final Document publish_mult = builder.parse(new InputSource(new StringReader(
				"<getpublishstatus status=\"ok\" time=\"1432436641\" multi=\"true\">" +
					"<list>" +
						"<item>" +
							"<stream>{動画の情報 省略}</stream>" +
							"<rtmp is_fms=\"1\">" +
								"<url>rtmp://host.live.nicovideo.jp:{ポート番号}/publicorigin/9876543210</url>" +
								"<stream>lv{数字}</stream>" +
								"<ticket>{ユーザID}:{ライブID}:{数字}:{数字}:{数字}:{数字}:{16進数}</ticket>" +
								"<bitrate>192000</bitrate>" +
							"</rtmp>" +
						"</item>"+
					"</list>" +
					"<user>{ユーザーの情報 省略}</user>" +
				"</getpublishstatus>"
			)));
			parsePublishStatus(publish_mult, true);
		} catch (final ParserConfigurationException e) {
			Log.w(TAG, e);
		} catch (final SAXException e) {
			Log.w(TAG, e);
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}

}
