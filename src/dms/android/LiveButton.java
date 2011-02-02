package dms.android;

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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class LiveButton extends TabActivity {
	public static final String PREF_SMS_CONTENT = "SMS";
	public static final String PREF_PHONE_NUMBER = "phoneNumber";
	private static final String PREF_MINUTES_REPEAT = "minutesRepeat";
	private static final String PREF_HOURS_REPEAT = "hoursRepeat";
	private static final String PREF_START_ON_BOOT = "startOnBoot";
	private static final String PREF_HOUR_FROM = "hourFrom";
	private static final String PREF_MINUTE_FROM = "minuteFrom";
	private static final String PREF_HOUR_UNTIL = "hourUntil";
	private static final String PREF_MINUTE_UNTIL = "minuteUntil";
	private static final String DEBUG_TAG = "LiveButtonActivity";
	private static final String PREFS_NAME = "LiveButtonPreferences";
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final int ACKNOWLEDGE_REQUEST_CODE = 1002;
	private static final int TIME_DIALOG_ID_START = 1;
	private static final int TIME_DIALOG_ID_STOP = 2;

	private Spinner mSpinHour;
	private Spinner mSpinMinute;
	private EditText mPhoneEntry;
	private EditText mSMSContent;
	private CheckBox mCbStartOnBoot;
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
		setContentView(R.layout.main);
		// settings singleton
		mSettings = getSharedPreferences(PREFS_NAME, 0);
		// controls
		mPhoneEntry = (EditText) findViewById(R.id.textPhone);
		mSMSContent = (EditText) findViewById(R.id.SMSContent);
		mSpinHour = (Spinner) findViewById(R.id.SpinnerHours);
		mSpinMinute = (Spinner) findViewById(R.id.SpinnerMinutes);
		mCbStartOnBoot = (CheckBox) findViewById(R.id.cbStartOnBoot);
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

		tabHost.addTab(tabHost.newTabSpec("Clock").setIndicator("Clock", res.getDrawable(R.drawable.tab_clock))
				.setContent(R.id.tabClock));
		tabHost.addTab(tabHost.newTabSpec("Message").setIndicator("Message", res.getDrawable(R.drawable.tab_message))
				.setContent(R.id.tabMessage));
		tabHost.addTab(tabHost.newTabSpec("Settings")
				.setIndicator("Settings", res.getDrawable(R.drawable.tab_settings)).setContent(R.id.tabSettings));

		setUICallbacks();

	}

	/**
	 * Set callbacks for UI elements
	 */
	private void setUICallbacks() {
		// button Pick contact
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
				String phoneNumber = mPhoneEntry.getText().toString();
				String SMS = mSMSContent.getText().toString();
				if (phoneNumber.length() > 0) {
					Intent intent = new Intent(LiveButton.this, HeartbeatReceiver.class);
					intent.putExtra(PREF_PHONE_NUMBER, phoneNumber);
					intent.putExtra(PREF_SMS_CONTENT, SMS);
					mSender = PendingIntent.getBroadcast(LiveButton.this, ACKNOWLEDGE_REQUEST_CODE, intent,
							PendingIntent.FLAG_UPDATE_CURRENT);

					AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
					int hours = Integer.parseInt(mSpinHour.getSelectedItem().toString());
					int minutes = Integer.parseInt(mSpinMinute.getSelectedItem().toString());
					long interval = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
					if (interval > 0) {
						alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, interval,
								mSender);
						writeSettings();
						Toast.makeText(LiveButton.this, "Live button monitor started", Toast.LENGTH_SHORT).show();
					} else {
						Toast
								.makeText(
										LiveButton.this,
										"You have to select a valid interval (one of the hour or minute field must be greater than zero).",
										Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(LiveButton.this, "Aborting no phone selected.", Toast.LENGTH_SHORT).show();
				}

			}
		});

		Button buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					// Cancel pending alarm
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
				showDialog(TIME_DIALOG_ID_START);
			}
		});

		Button buttonPickStop = (Button) findViewById(R.id.buttonPickUntil);
		buttonPickStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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
		// Start on boot
		boolean startOnBoot = mSettings.getBoolean(PREF_START_ON_BOOT, false);
		mCbStartOnBoot.setChecked(startOnBoot);
		// Time
		int hours = mSettings.getInt(PREF_HOURS_REPEAT, 0);
		int minutes = mSettings.getInt(PREF_MINUTES_REPEAT, 0);
		mSpinHour.setSelection(hours);
		mSpinMinute.setSelection(minutes);
		// Start time
		mHourFrom = mSettings.getInt(PREF_HOUR_FROM, 9);
		mMinuteFrom = mSettings.getInt(PREF_MINUTE_FROM, 0);
		// Stop time
		mHourUntil = mSettings.getInt(PREF_HOUR_UNTIL, 21);
		mMinuteUntil = mSettings.getInt(PREF_MINUTE_UNTIL, 0);
		// Refresh view
		updateDisplay();
		// call details
		String phoneNumber = mSettings.getString(PREF_PHONE_NUMBER, "");
		mPhoneEntry.setText(phoneNumber);
		String SMS = mSettings.getString(PREF_SMS_CONTENT, getText(R.string.SMSContent).toString());
		mSMSContent.setText(SMS);

	}

	private void writeSettings() {
		// Save preferences
		SharedPreferences.Editor editor = mSettings.edit();
		// Start on boot
		editor.putBoolean(PREF_START_ON_BOOT, mCbStartOnBoot.isChecked());
		// Time
		editor.putInt(PREF_HOURS_REPEAT, mSpinHour.getSelectedItemPosition());
		editor.putInt(PREF_MINUTES_REPEAT, mSpinMinute.getSelectedItemPosition());
		// Start time
		editor.putInt(PREF_HOUR_FROM, mHourFrom);
		editor.putInt(PREF_MINUTE_FROM, mMinuteFrom);
		// Stop time
		editor.putInt(PREF_HOUR_UNTIL, mHourUntil);
		editor.putInt(PREF_MINUTE_UNTIL, mMinuteUntil);

		// call details
		editor.putString(PREF_PHONE_NUMBER, mPhoneEntry.getText().toString());
		editor.putString(PREF_SMS_CONTENT, mSMSContent.getText().toString());
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
						Log.w(DEBUG_TAG, "No results");
					}
				} catch (Exception e) {
					Log.e(DEBUG_TAG, "Failed to get phone data", e);
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
			Log.w(DEBUG_TAG, "Warning: activity result not ok");
		}
	}

}