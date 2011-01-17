package dms.android;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LiveButton extends Activity {
	private static final String PREF_PHONE_NUMBER = "phoneNumber";
	private static final String PREF_MINUTES_REPEAT = "minutesRepeat";
	private static final String PREF_HOURS_REPEAT = "hoursRepeat";
	private static final String PREF_START_ON_BOOT = "startOnBoot";
	//private static final String DEBUG_TAG = "LiveButtonActivity";
	private static final String PREFS_NAME = "LiveButtonPreferences";
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final int ACKNOWLEDGE_REQUEST_CODE = 1002;
	private Spinner spinHour;
	private Spinner spinMinute;
	private EditText phoneEntry;
	private CheckBox cbStartOnBoot;
	PendingIntent sender;
	SharedPreferences settings; 
	//TODO: Add saving state
	//TODO: Add starting on telephone boot
	//TODO: Add icon on notification bar
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//settings singleton
		settings = getSharedPreferences(PREFS_NAME, 0);
		//controls
		phoneEntry = (EditText) findViewById(R.id.textPhone);
		spinHour = (Spinner) findViewById(R.id.SpinnerHours);
		spinMinute = (Spinner) findViewById(R.id.SpinnerMinutes);
		cbStartOnBoot = (CheckBox)findViewById(R.id.cbStartOnBoot);
		//fill controls
		ArrayAdapter<?> adapter =  ArrayAdapter.createFromResource(this,
				R.array.hourArray, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinHour.setAdapter(adapter);
		
		ArrayAdapter<?> adapter2 = ArrayAdapter.createFromResource(this,
				R.array.minuteArray, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinMinute.setAdapter(adapter2);
		

		// button Pick contact
		Button buttonPick = (Button) findViewById(R.id.buttonPick);

		buttonPick.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO: Pick contact
				startActivityForResult(new Intent(Intent.ACTION_PICK,
						Phone.CONTENT_URI), CONTACT_PICKER_RESULT);
			}
		});

		Button buttonActivar = (Button) findViewById(R.id.buttonStart);
		buttonActivar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//schedule alarm
				String phoneNumber = phoneEntry.getText().toString();
				if (phoneNumber.length() > 0) {
					Intent intent = new Intent(LiveButton.this,
							HeartbeatReceiver.class);
					intent.putExtra(PREF_PHONE_NUMBER, phoneNumber);
					sender = PendingIntent.getBroadcast(LiveButton.this,
							ACKNOWLEDGE_REQUEST_CODE, intent,
							PendingIntent.FLAG_UPDATE_CURRENT);

					AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
					int hours = Integer.parseInt(spinHour.getSelectedItem().toString());
					int minutes = Integer.parseInt(spinMinute.getSelectedItem().toString());
					long interval = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
					alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System
							.currentTimeMillis() + 1000, interval, sender);
					writeSettings();
				} else {
					Toast.makeText(LiveButton.this,
							"Aborting no phone selected.", Toast.LENGTH_SHORT)
							.show();
				}

			}
		});

		Button buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					//Cancel pending alarm
					Intent intent = new Intent(LiveButton.this,
							HeartbeatReceiver.class);

					sender = PendingIntent.getBroadcast(LiveButton.this,
							ACKNOWLEDGE_REQUEST_CODE, intent,
							PendingIntent.FLAG_UPDATE_CURRENT);
					AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
					alarmManager.cancel(sender);
					sender = null;
				} catch (Exception e) {
					Toast.makeText(LiveButton.this,
							"Live button monitor not started.",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		readSettings();
		
	}
	@Override
	public void onDestroy() {
		//write settings on exit
		super.onDestroy();
		writeSettings();
	}

	private void readSettings() {
		//Rescue saved preferences
		//Start on boot
		boolean startOnBoot = settings.getBoolean(PREF_START_ON_BOOT, false);		
		cbStartOnBoot.setChecked(startOnBoot);
		//Time
		int hours = settings.getInt(PREF_HOURS_REPEAT, 0);
		int minutes = settings.getInt(PREF_MINUTES_REPEAT, 0);
		spinHour.setSelection(hours);
		spinMinute.setSelection(minutes);
		//call details
		String phoneNumber = settings.getString(PREF_PHONE_NUMBER, "");
		phoneEntry.setText(phoneNumber);
	}

	private void writeSettings() {
		//Save preferences
		SharedPreferences.Editor editor = settings.edit();
		//Start on boot		
		editor.putBoolean(PREF_START_ON_BOOT, cbStartOnBoot.isChecked());				
		//Time
		editor.putInt(PREF_HOURS_REPEAT, spinHour.getSelectedItemPosition());
		editor.putInt(PREF_MINUTES_REPEAT, spinMinute.getSelectedItemPosition());
				
		//call details
		editor.putString(PREF_PHONE_NUMBER, phoneEntry.getText().toString());
		editor.commit();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// super.onActivityResult(requestCode, resultCode, data);
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
					cursor = getContentResolver().query(Phone.CONTENT_URI,
							null, Phone._ID + "=?", new String[] { id }, null);

					int phoneIdx = cursor.getColumnIndex(Phone.DATA);
					// Cursor should only have one/zero row, but let's just
					// ensure.
					if (cursor.moveToFirst()) {
						phone = cursor.getString(phoneIdx);

					} else {
						// Log.w(DEBUG_TAG, "No results");
					}
				} catch (Exception e) {
					// Log.e(DEBUG_TAG, "Failed to get phone data", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					phoneEntry.setText(phone);
					if (phone.length() == 0) {
						Toast.makeText(this, "No phone found.",
								Toast.LENGTH_LONG).show();
					}

				}

				break;
			}

		} else {
			// Log.w(DEBUG_TAG, "Warning: activity result not ok");
		}
	}

}