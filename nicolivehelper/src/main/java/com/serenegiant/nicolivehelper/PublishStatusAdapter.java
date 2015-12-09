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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class PublishStatusAdapter extends ArrayAdapter<PublishInfo> {
	private final int mLayoutId;
	private final LayoutInflater mInflater;

	private static class ViewHolder {
		private TextView title;
		private TextView url;
		private TextView stream;
		private TextView bitrate;
	}

	public PublishStatusAdapter(@NonNull final Context context, @LayoutRes final int layout_id) {
		super(context, layout_id);
		mLayoutId = layout_id;
		mInflater = LayoutInflater.from(context);
	}

	public PublishStatusAdapter(@NonNull final Context context, @LayoutRes final int layout_id, final PublishInfo[] objects) {
		super(context, layout_id, objects);
		mLayoutId = layout_id;
		mInflater = LayoutInflater.from(context);
	}

	public PublishStatusAdapter(@NonNull final Context context, @LayoutRes final int layout_id, final List<PublishInfo> objects) {
		super(context, layout_id, objects);
		mLayoutId = layout_id;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View result = convertView;
		if (result == null) {
			result = mInflater.inflate(mLayoutId, parent, false);
		}
		ViewHolder holder = (ViewHolder)result.getTag(R.id.PublishStatusAdapter_viewholder_id);
		if (holder == null) {
			holder = new ViewHolder();
			holder.title = (TextView)result.findViewById(R.id.title);
			holder.url = (TextView)result.findViewById(R.id.url);
			holder.stream = (TextView)result.findViewById(R.id.stream);
			holder.bitrate = (TextView)result.findViewById(R.id.bitrate);
		}
		final PublishInfo info = getItem(position);
		if (holder.title != null) {
			holder.title.setText(info.title);
		}
		if (holder.url != null) {
			holder.url.setText(info.url);
		}
		if (holder.stream != null) {
			holder.stream.setText(info.stream);
		}
		if (holder.bitrate != null) {
			holder.bitrate.setText(Integer.toString(info.bitrate));
		}
		return result;
	}
}
