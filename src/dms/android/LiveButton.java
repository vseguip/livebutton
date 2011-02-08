package dms.android;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
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
	private static final int TIME_DIALOG_ID_START = 1;
	private static final int TIME_DIALOG_ID_STOP = 2;

	private Spinner mSpinHour;
	private Spinner mSpinMinute;
	private EditText mPhoneEntry;
	private EditText mSMSContent;
	private PendingIntent mSender;
	private SharedPreferences mSettings;
	private TextView mTextFrom;
	private TextView mTextUntil;

	private int mHourFrom;
	private int mMinuteFrom;

	private int mHourUntil;
	private int mMinuteUntil;

	private TimePickerDialog.OnTimeSetListener mTimeSetListenerStart = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHourFrom = hourOfDay;
			mMinuteFrom = minute;
			updateDisplay();
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListenerStop = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHourUntil = hourOfDay;
			mMinuteUntil = minute;
			updateDisplay();
		}
	};

	// TODO: Add starting on telephone boot
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
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this, R.array.hourArray,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinHour.setAdapter(adapter);

		ArrayAdapter<?> adapter2 = ArrayAdapter.createFromResource(this, R.array.minuteArray,
				android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinMinute.setAdapter(adapter2);

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		Log.i(LOG_TAG, "Adding clock tab");
		tabHost.addTab(tabHost.newTabSpec("Clock").setIndicator("Clock", res.getDrawable(R.drawable.tab_clock))
				.setContent(R.id.tabClock));
		Log.i(LOG_TAG, "Adding messagetab");
		tabHost.addTab(tabHost.newTabSpec("Message").setIndicator("Message", res.getDrawable(R.drawable.tab_message))
				.setContent(R.id.tabMessage));
		Log.i(LOG_TAG, "Adding settings tab");
		tabHost.addTab(tabHost.newTabSpec("Settings")
				.setIndicator("Settings", res.getDrawable(R.drawable.tab_settings)).setContent(
						new Intent(this, SettingsActivity.class)));
		setUICallbacks();

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
					int hours = Integer.parseInt(mSpinHour.getSelectedItem().toString());
					int minutes = Integer.parseInt(mSpinMinute.getSelectedItem().toString());
					long interval = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
					if (interval > 0) {
						Log.i(LOG_TAG, "Scheduling alarm");
						writeSettings();
						AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
						Intent intent = new Intent(LiveButton.this, HeartbeatReceiver.class);
						mSender = PendingIntent.getBroadcast(LiveButton.this, ACKNOWLEDGE_REQUEST_CODE, intent,
								PendingIntent.FLAG_UPDATE_CURRENT);
						Date day = Calendar.getInstance().getTime();
						day.setHours(mHourFrom);
						day.setMinutes(mMinuteFrom);
						day.setSeconds(0);
						alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, day.getTime(), interval, mSender);

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

					mSender = PendingIntent.getBroadcast(LiveButton.this, ACKNOWLEDGE_REQUEST_CODE, intent,
							PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_NO_CREATE);

					AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
					alarmManager.cancel(mSender);
					mSender = null;
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
				showDialog(TIME_DIALOG_ID_START);
			}
		});

		Button buttonPickStop = (Button) findViewById(R.id.buttonPickUntil);
		buttonPickStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "Selecting stop time");
				showDialog(TIME_DIALOG_ID_STOP);
			}
		});
		readSettings();
	}

	private void updateDisplay() {
		mTextFrom.setText(new StringBuilder().append(Utils.pad(mHourFrom)).append(":").append(Utils.pad(mMinuteFrom)));
		mTextUntil.setText(new StringBuilder().append(Utils.pad(mHourUntil)).append(":")
				.append(Utils.pad(mMinuteUntil)));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID_START:
			return new TimePickerDialog(this, mTimeSetListenerStart, mHourFrom, mMinuteFrom, false);
		case TIME_DIALOG_ID_STOP:
			return new TimePickerDialog(this, mTimeSetListenerStop, mHourUntil, mMinuteUntil, false);
		}
		return null;
	}

	@Override
	public void onDestroy() {
		// write settings on exit
		super.onDestroy();
		writeSettings();
	}

	private void readSettings() {
		// Rescue saved preferences
		// Time
		Log.i(LOG_TAG, "Reading settings");
		int hours = mSettings.getInt(getString(R.string.hoursRepeatPref), 0);
		int minutes = mSettings.getInt(getString(R.string.minutesRepeatPref), 0);
		mSpinHour.setSelection(hours);
		mSpinMinute.setSelection(minutes);
		// Start time
		mHourFrom = mSettings.getInt(getString(R.string.hourFromPref), 9);
		mMinuteFrom = mSettings.getInt(getString(R.string.minuteFromPref), 0);
		// Stop time
		mHourUntil = mSettings.getInt(getString(R.string.hourUntilPref), 21);
		mMinuteUntil = mSettings.getInt(getString(R.string.minuteUntilPref), 0);
		// Refresh view
		updateDisplay();
		// call details
		String phoneNumber = mSettings.getString(getString(R.string.phoneNumberPref), "");
		mPhoneEntry.setText(phoneNumber);
		String SMS = mSettings.getString(getString(R.string.SMSContentPref), getString(R.string.SMSContent));
		mSMSContent.setText(SMS);

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

		// call details
		editor.putString(getString(R.string.phoneNumberPref), mPhoneEntry.getText().toString());
		editor.putString(getString(R.string.SMSContentPref), mSMSContent.getText().toString());

		editor.commit();
	}

	/*
	 * Get the result for tghe phone contact picker activity
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
					cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone._ID + "=?", new String[] { id },
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