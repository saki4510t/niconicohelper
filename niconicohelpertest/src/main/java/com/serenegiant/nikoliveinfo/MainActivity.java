package com.serenegiant.nikoliveinfo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.niconicohelper.NicoliveHelper;
import com.serenegiant.niconicohelper.PublishInfo;
import com.serenegiant.niconicohelper.PublishStatusAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
	private static final boolean DEBUG = true;	// FIXME 実同時はfalseにすること
	private static final String TAG = MainActivity.class.getSimpleName();

	private PublishStatusAdapter mAdapter;
	private NicoliveHelper mHelper;
	private volatile boolean isRequesting;
//	private boolean mSavePassword;
	private EditText mAddressEdit;
	private EditText mPasswordEdit;
	private final List<View> mViews = new ArrayList<View>();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final ListView lv = (ListView)findViewById(R.id.listView);
		final TextView tv = (TextView)findViewById(R.id.empty);
		lv.setEmptyView(tv);
		mViews.clear();
		final Button button = (Button)findViewById(R.id.get_info_button);
		button.setOnClickListener(mOnClickListener);
		mViews.add(button);
//		final CheckBox checkbox = (CheckBox)findViewById(R.id.save_password_checkBox);
//		checkbox.setChecked(false);
//		checkbox.setOnCheckedChangeListener(mOnCheckedChangeListener);
//		mViews.add(checkbox);
		mAddressEdit = (EditText)findViewById(R.id.address_edittext);
		mViews.add(mAddressEdit);
		mPasswordEdit = (EditText)findViewById(R.id.password_edittext);
		mViews.add(mPasswordEdit);
		mAdapter = new PublishStatusAdapter(this, R.layout.list_item_publish_info);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// FIXME パスワードを保存しているときはリストアする
		updateButtons();
	}

	@Override
	protected void onPause() {
		if (mHelper != null) {
			mHelper.release();
			mHelper = null;
		}
		super.onPause();
	}

	private final NicoliveHelper.Callback mCallback = new NicoliveHelper.Callback() {
		@Override
		public void onLogin(final NicoliveHelper helper) {
			if (DEBUG) Log.v(TAG, "onLogin:");
			helper.requestPublishStatus(true);
//			if (mSavePassword) {
//				final String addr = mAddressEdit.getText().toString();
//				final String pass = mPasswordEdit.getText().toString();
//				// FIXME パスワードの暗号化と保存
//			}
		}

		@Override
		public void onUpdatePublishInfo(final NicoliveHelper helper, final List<PublishInfo> info) {
			if (DEBUG) Log.v(TAG, "onUpdatePublishInfo:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mAdapter != null) {
						mAdapter.clear();
						mAdapter.addAll(info);
					}
				}
			});
			isRequesting = false;
			updateButtons();
		}

		@Override
		public void onError(final NicoliveHelper helper, final Exception e) {
			if (DEBUG) Log.v(TAG, "onError:" + e);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, R.string.err_get, Toast.LENGTH_SHORT).show();
				}
			});
			if (mHelper != null) {
				mHelper.release();
				mHelper = null;
			}
			isRequesting = false;
			updateButtons();
		}
	};

	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.get_info_button:
				if (mHelper == null) {
					mHelper = new NicoliveHelper(MainActivity.this, mCallback);
				}
				// メールアドレスとパスワードを取得
				try {
					final String addr = mAddressEdit.getText().toString();
					final String pass = mPasswordEdit.getText().toString();
					isRequesting = true;
					// ログイン実行
					mHelper.login(addr, pass);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				updateButtons();
				break;
			}
		}
	};

//	private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
//		= new CompoundButton.OnCheckedChangeListener() {
//		@Override
//		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
//			mSavePassword = isChecked;
//		}
//	};

	private void updateButtons() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (final View view: mViews) {
					view.setEnabled(!isRequesting);
				}
			}
		});
	}
}
