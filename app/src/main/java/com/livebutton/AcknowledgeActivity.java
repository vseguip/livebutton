/**
 *
 * Copyright (c) 2015 Vicent Segui
 * Distributed under the GNU GPL v2. For full terms see the file gpl.txt
 *
 */
package com.livebutton;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AcknowledgeActivity extends Activity {
	final static int CALL_REQUEST = 1003;
	private String LOG_TAG = "AcknowledgeActivity";
	private Vibrator mVib;
	private CountDownTimer mTimer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "Creating acknowledge activity");
		setContentView(R.layout.acknowledge);
		Log.i(LOG_TAG, "Getting prefs");
		mVib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		final String phoneNumber = getIntent().getStringExtra(getString(R.string.phoneNumberPref));
		final String sms = getIntent().getStringExtra(getString(R.string.SMSContentPref));
		int countdown = getIntent().getIntExtra(getString(R.string.countDownTimerPref), 10);
		String ringtone = getIntent().getStringExtra(getString(R.string.ringtonePref));
		final boolean vibrate = getIntent().getBooleanExtra(getString(R.string.vibratePref), false);
		if (countdown < 1)
			countdown = 10;
		final TextView textCounter = (TextView) findViewById(R.id.timeRemaining);
		textCounter.setText(Integer.toString(countdown));
		Utils.playAlarm(this, ringtone);
		mTimer = new CountDownTimer(countdown * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				textCounter.setText(Long.toString(millisUntilFinished / 1000));
			}

			@Override
			public void onFinish() {
				Log.i(LOG_TAG, "Countdown ended without user feedback. Sending message.");
				Utils.stopAlarm();
				if (vibrate && (mVib != null)) {
					mVib.cancel();
				}
				Intent newIntent = new Intent(AcknowledgeActivity.this, SendMessageService.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				newIntent.putExtra("phoneNumber", phoneNumber);
				newIntent.putExtra("SMS", sms);
				AcknowledgeActivity.this.startService(newIntent);
				Log.i(LOG_TAG, "Finish activity.");
				finish();
			}
		};

		Button buttonAck = (Button) findViewById(R.id.buttonCancelCall);
		buttonAck.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "Countdown canceled");
				mTimer.cancel();
				if (vibrate && (mVib != null)) {
					mVib.cancel();
				}
				Utils.stopAlarm();
				Log.i(LOG_TAG, "Finishing AcknowledgeActivity");
				AcknowledgeActivity.this.finish();
			}
		});
		Log.i(LOG_TAG, "Starting countdown");
		if (vibrate && (mVib != null)) {
			mVib.vibrate(countdown * 1000);
		}
		mTimer.start();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mTimer != null)
			mTimer.cancel();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTimer != null)
			mTimer.cancel();
	}
}
