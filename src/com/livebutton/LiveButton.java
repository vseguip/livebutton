package com.livebutton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class LiveButton extends TabActivity {

	private static final String LOG_TAG = "LiveButtonActivity";
	// public static final String PREFS_NAME = "LiveButtonPreferences";
	private static final int CONTACT_PICKER_RESULT = 1001;
	static final int ACKNOWLEDGE_REQUEST_CODE = 1002;

	private Spinner mSpinHour;
	private Spinner mSpinMinute;
	private EditText mPhoneEntry;
	private EditText mSMSContent;
	private PendingIntent mSender;
	private SharedPreferences mSettings;
	private TextView mTextFrom;
	private TextView mTextUntil;

	private long mNextAlarm;
	private int mHourFrom;
	private int mMinuteFrom;

	private int mHourUntil;
	private int mMinuteUntil;

	private Handler mHandler;

	Runnable mStatusChecker = new Runnable() {
		@Override
		public void run() {
			updateUIWithSettings();
			long now = System.currentTimeMillis();
			long updateInterval = 30*1000;
			long offset = updateInterval-(now%updateInterval);
			mHandler.postDelayed(mStatusChecker, offset);
		}
	};

	// TODO: Add icon on notification bar
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "Creating LiveButtonActivity");
		setContentView(R.layout.main);
		// settings singleton
		Log.i(LOG_TAG, "Getting settings");
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);// getSharedPreferences(PREFS_NAME,
		// 0);
		// controls
		mPhoneEntry = (EditText) findViewById(R.id.textPhone);
		mSMSContent = (EditText) findViewById(R.id.SMSContent);
		mSpinHour = (Spinner) findViewById(R.id.SpinnerHours);
		mSpinMinute = (Spinner) findViewById(R.id.SpinnerMinutes);
		mTextFrom = (TextView) findViewById(R.id.textFrom);
		mTextUntil = (TextView) findViewById(R.id.textUntil);
		// fill controls
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(
																	this,
																	R.array.hourArray,
																	android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinHour.setAdapter(adapter);

		ArrayAdapter<?> adapter2 = ArrayAdapter.createFromResource(
																	this,
																	R.array.minuteArray,
																	android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinMinute.setAdapter(adapter2);

		Resources res = getResources();
		TabHost tabHost = getTabHost(); // The activity TabHost
		Log.i(LOG_TAG, "Adding clock tab");
		tabHost.addTab(tabHost.newTabSpec("Clock")
				.setIndicator(getString(R.string.tabClockName), res.getDrawable(R.drawable.tab_clock))
				.setContent(R.id.tabClock));
		Log.i(LOG_TAG, "Adding messagetab");
		tabHost.addTab(tabHost.newTabSpec("Message")
				.setIndicator(getString(R.string.tabMessageName), res.getDrawable(R.drawable.tab_message))
				.setContent(R.id.tabMessage));
		Log.i(LOG_TAG, "Adding settings tab");
		tabHost.addTab(tabHost.newTabSpec("Settings")
				.setIndicator(getString(R.string.tabSettingsName), res.getDrawable(R.drawable.tab_settings))
				.setContent(new Intent(this, SettingsActivity.class)));
		mHandler = new Handler();
		setUICallbacks();

	}

	void startRepeatingTask() {
		mStatusChecker.run();
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
	}

	private void updateUIWithSettings() {
		TextView nextAlarm = (TextView) findViewById(R.id.nextAlarm);
		StringBuilder str = new StringBuilder();
		if (mNextAlarm > 0) {
			Date day = Calendar.getInstance().getTime();
			long currentTime = day.getTime();
			int hours = Integer.parseInt(mSpinHour.getSelectedItem().toString());
			int minutes = Integer.parseInt(mSpinMinute.getSelectedItem().toString());
			long interval = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
			Intent intent = new Intent(getApplicationContext(), HeartbeatReceiver.class);
			PendingIntent pending = PendingIntent.getBroadcast(	getApplicationContext(),
																ACKNOWLEDGE_REQUEST_CODE,
																intent,
																PendingIntent.FLAG_NO_CREATE);
			
			if (pending != null) {
				Log.i(LOG_TAG, "Found intent: " + pending.getIntentSender().toString() +" => "+ pending.getTargetPackage());
				str.append(getString(R.string.nextAlarm));
				str.append(" ").append(DateUtils.getRelativeTimeSpanString(mNextAlarm, currentTime, 0));
			}  else {
				mNextAlarm=-1;
				writeSettings();
			}
		}
		if(str.length()==0) {
			str.append(getString(R.string.noAlarm));
		}
		nextAlarm.setText(str.toString());
	}

	/**
	 * Set callbacks for UI elements
	 */
	private void setUICallbacks() {
		// button Pick contact
		Log.i(LOG_TAG, "Setting UI callbacks");
		Button buttonPick = (Button) findViewById(R.id.buttonPick);

		buttonPick.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO: Pick contact
				startActivityForResult(new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI), CONTACT_PICKER_RESULT);
			}
		});

		Button buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// schedule alarm
				Log.i(LOG_TAG, "User clicked start");
				String phoneNumber = mPhoneEntry.getText().toString();
				if (phoneNumber.length() > 0) {				
					if (setupAlarm(true)) {
						Log.i(LOG_TAG, "Scheduling alarm");
						writeSettings();
						Log.i(LOG_TAG, "Live button monitor started");
						Toast.makeText(LiveButton.this, "Live button monitor started", Toast.LENGTH_SHORT).show();
					} else {
						Log.i(LOG_TAG, "No valid interval when starting alarm");
						Toast
								.makeText(
											LiveButton.this,
											"You have to select a valid interval (one of the hour or minute field must be greater than zero).",
											Toast.LENGTH_SHORT).show();
					}
				} else {
					Log.i(LOG_TAG, "No phone selected, can't schedule wakeup");
					Toast.makeText(LiveButton.this, "No phone selected, can't schedule wakeup", Toast.LENGTH_SHORT)
							.show();
				}

			}
		});

		Button buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					// Cancel pending alarm
					Log.i(LOG_TAG, "Canceling pending alarm");
					Intent intent = new Intent(LiveButton.this, HeartbeatReceiver.class);

					mSender = PendingIntent.getBroadcast(
															LiveButton.this,
															ACKNOWLEDGE_REQUEST_CODE,
															intent,
															PendingIntent.FLAG_UPDATE_CURRENT
																	| PendingIntent.FLAG_NO_CREATE);

					AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
					alarmManager.cancel(mSender);
					mSender = null;
					mNextAlarm = -1;
					writeSettings();
					Toast.makeText(LiveButton.this, "Live button monitor stopped.", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(LiveButton.this, "Live button monitor not started.", Toast.LENGTH_SHORT).show();
				}
			}
		});

		Button buttonPickStart = (Button) findViewById(R.id.buttonPickFrom);
		buttonPickStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "Selecting start time");
				new TimePickerDialog(LiveButton.this, new TimePickerDialog.OnTimeSetListener() {

					@Override
					public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
						mHourFrom = hourOfDay;
						mMinuteFrom = minute;
						mTextFrom.setText(new StringBuilder().append(Utils.pad(mHourFrom)).append(":")
								.append(Utils.pad(mMinuteFrom)));
					}
				}, mHourFrom, mMinuteFrom, false).show();
			}
		});

		Button buttonPickStop = (Button) findViewById(R.id.buttonPickUntil);
		buttonPickStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "Selecting stop time");
				new TimePickerDialog(LiveButton.this, new TimePickerDialog.OnTimeSetListener() {

					@Override
					public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
						mHourUntil = hourOfDay;
						mMinuteUntil = minute;
						mTextUntil.setText(new StringBuilder().append(Utils.pad(mHourUntil)).append(":")
								.append(Utils.pad(mMinuteUntil)));
					}
				}, mHourUntil, mMinuteUntil, false).show();
			}
		});
		readSettings();
	}

	@Override
	public void onDestroy() {
		// write settings on exit
		super.onDestroy();
		stopRepeatingTask();
		writeSettings();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopRepeatingTask();
		writeSettings();
	}

	@Override
	public void onResume() {
		super.onResume();
		readSettings();
		startRepeatingTask();
	}

	private void readSettings() {
		// Rescue saved preferences
		// Time
		Log.i(LOG_TAG, "Reading settings");
		int hoursIndex = mSettings.getInt(getString(R.string.hoursRepeatPref), 0);
		int minutesIndex = mSettings.getInt(getString(R.string.minutesRepeatPref), 1);
		mSpinHour.setSelection(hoursIndex);
		mSpinMinute.setSelection(minutesIndex);
		// Start time
		mHourFrom = mSettings.getInt(getString(R.string.hourFromPref), 9);
		mMinuteFrom = mSettings.getInt(getString(R.string.minuteFromPref), 0);
		// Stop time
		mHourUntil = mSettings.getInt(getString(R.string.hourUntilPref), 21);
		mMinuteUntil = mSettings.getInt(getString(R.string.minuteUntilPref), 0);
		// Next alarm scheduled
		mNextAlarm = mSettings.getLong(getString(R.string.timeScheduled), -1);

		// Refresh view
		mTextFrom.setText(new StringBuilder().append(Utils.pad(mHourFrom)).append(":").append(Utils.pad(mMinuteFrom)));
		mTextUntil.setText(new StringBuilder().append(Utils.pad(mHourUntil)).append(":")
				.append(Utils.pad(mMinuteUntil)));
		// call details
		String phoneNumber = mSettings.getString(getString(R.string.phoneNumberPref), "");
		mPhoneEntry.setText(phoneNumber);
		String SMS = mSettings.getString(getString(R.string.SMSContentPref), getString(R.string.SMSContent));
		mSMSContent.setText(SMS);
		
		Intent intent = new Intent(getApplicationContext(), HeartbeatReceiver.class);
		mSender = PendingIntent.getBroadcast(	getApplicationContext(),
															ACKNOWLEDGE_REQUEST_CODE,
															intent,
															PendingIntent.FLAG_UPDATE_CURRENT);
		setupAlarm(false);
		updateUIWithSettings();
	}

	/**
	 * @param hours
	 * @param minutes
	 */
	private boolean setupAlarm(boolean create) {
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		if((mNextAlarm > 0)||create){
			int hours = Integer.parseInt(mSpinHour.getSelectedItem().toString());
			int minutes = Integer.parseInt(mSpinMinute.getSelectedItem().toString());
			if((hours == 0) &&(minutes == 0) )
				return false;
			Date day = Calendar.getInstance().getTime();
			long currentTime = day.getTime();
			day.setHours(mHourFrom);
			day.setMinutes(mMinuteFrom);
			day.setSeconds(0);
			mNextAlarm = day.getTime();
			long interval = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
			while(mNextAlarm<=currentTime)
				mNextAlarm+=interval;

			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mNextAlarm, interval, mSender);
			
		} else {
			alarmManager.cancel(mSender);
			return false;
		}
		return true;
	}

	private void writeSettings() {
		Log.i(LOG_TAG, "Writing settings");
		// Save preferences
		SharedPreferences.Editor editor = mSettings.edit();
		// Start on boot
		// Time
		editor.putInt(getString(R.string.hoursRepeatPref), mSpinHour.getSelectedItemPosition());
		editor.putInt(getString(R.string.minutesRepeatPref), mSpinMinute.getSelectedItemPosition());
		// Start time
		editor.putInt(getString(R.string.hourFromPref), mHourFrom);
		editor.putInt(getString(R.string.minuteFromPref), mMinuteFrom);
		// Stop time
		editor.putInt(getString(R.string.hourUntilPref), mHourUntil);
		editor.putInt(getString(R.string.minuteUntilPref), mMinuteUntil);

		editor.putLong(getString(R.string.timeScheduled), mNextAlarm);
		// call details
		editor.putString(getString(R.string.phoneNumberPref), mPhoneEntry.getText().toString());
		editor.putString(getString(R.string.SMSContentPref), mSMSContent.getText().toString());

		editor.commit();
		updateUIWithSettings();
	}

	/*
	 * Get the result for the phone contact picker activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Log.i(LOG_TAG, "Contact chosen");
				Cursor cursor = null;
				String phone = "";
				try {

					Uri result = data.getData();
					// get the contact id from the Uri
					String id = result.getLastPathSegment();
					// query for the exact Phone ID
					cursor = getContentResolver().query(
														Phone.CONTENT_URI,
														null,
														Phone._ID + "=?",
														new String[] { id },
														null);

					int phoneIdx = cursor.getColumnIndex(Phone.DATA);
					// Cursor should only have one/zero row, but let's just
					// ensure.
					if (cursor.moveToFirst()) {
						phone = cursor.getString(phoneIdx);

					} else {
						Log.w(LOG_TAG, "No results");
					}
				} catch (Exception e) {
					Log.e(LOG_TAG, "Failed to get phone data" + Log.getStackTraceString(e));
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					mPhoneEntry.setText(phone);
					if (phone.length() == 0) {
						Toast.makeText(this, "No phone found.", Toast.LENGTH_LONG).show();
					}

				}

				break;
			}

		} else {
			Log.e(LOG_TAG, "Warning: activity result not ok");
		}
	}

}