package com.livebutton;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class HeartbeatReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "HeartbeatReceiver";
	SharedPreferences mSettings;
	private int mHourUntil;
	private int mMinuteUntil;
	private int mHourFrom;
	private int mMinuteFrom;
	private String phoneNumber;
	private String sms;
	private int mCountdown;
	private long mInterval;
	private String mRingtone;
	private boolean mStartOnBoot;
	private boolean mVibrate;
	private boolean mStartCurrent;

	private void readSettings(Context context) {
		Log.i(LOG_TAG, "Reading settings");
		// Rescue saved preferences
		// Time
		if (mSettings == null)
			mSettings = PreferenceManager.getDefaultSharedPreferences(context);
		int hour = mSettings.getInt(context.getString(R.string.hoursRepeatPref), 0);
		int minute = mSettings.getInt(context.getString(R.string.minutesRepeatPref), 0);
		
		String hours [] =context.getResources().getStringArray(R.array.hourArray);
		String minutes[] = context.getResources().getStringArray(R.array.minuteArray);
		mInterval = (Integer.parseInt(hours[hour])*60) + Integer.parseInt(minutes[minute]);
		mInterval *= 60 * 1000;// to millis
		if (mInterval == 0) {
			mInterval = 3600 * 1000;// 1 hour at least
		}
		// Start time
		mHourFrom = mSettings.getInt(context.getString(R.string.hourFromPref), 9);
		mMinuteFrom = mSettings.getInt(context.getString(R.string.minuteFromPref), 0);
		// Stop time
		mHourUntil = mSettings.getInt(context.getString(R.string.hourUntilPref), 21);
		mMinuteUntil = mSettings.getInt(context.getString(R.string.minuteUntilPref), 0);
		// countdown timer & ringtone
		// parse string because Android API doesn't allow for integer arrays to
		// be used as preferences. Duh!
		mCountdown = Integer.parseInt(mSettings.getString(context.getString(R.string.countDownTimerPref), "10"));
		mRingtone = mSettings.getString(context.getString(R.string.ringtonePref), RingtoneManager.getDefaultUri(
				RingtoneManager.TYPE_RINGTONE).toString());
		// call details
		phoneNumber = mSettings.getString(context.getString(R.string.phoneNumberPref), "");
		sms = mSettings.getString(context.getString(R.string.SMSContentPref), context.getString(R.string.SMSContent));
		// Start on boot active?
		mStartOnBoot = mSettings.getBoolean(context.getString(R.string.startOnBootPref), false);
		mVibrate = mSettings.getBoolean(context.getString(R.string.vibratePref), false);
		mStartCurrent = mSettings.getBoolean(context.getString(R.string.startCurrent), false);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		
		readSettings(context);

		Date now = Calendar.getInstance().getTime();
		Date max = Calendar.getInstance().getTime();
		now.setSeconds(0);
		max.setSeconds(0);
		max.setHours(mHourUntil);
		max.setMinutes(mMinuteUntil);
		
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Log.i(LOG_TAG, "Receiving boot completed. Schedule alarm");
			//Called because we just boot completed
			startOnBoot(context, intent);
		} else {
			Log.i(LOG_TAG, "Receiving timer");
			if (now.after(max)) {				
				// Stop for today
				Log.i(LOG_TAG, "Past maximum time. Scheduling for tomorrow");
				rescheduleAlarm(context, intent);
			} else {
				// Start utility
				Log.i(LOG_TAG, "Starting AcknowledgeActivity");
				startAcknowledgeActivity(context, intent);
			}
		}
	}

	/** Start the acknowledge activity to check if the user is responding
	 * 
	 * @param context Context that called the receiver
	 * @param intent Intent of the call
	 */
	private void startAcknowledgeActivity(Context context, Intent intent) {
		// Check we are a ghost from the past...
		Log.i(LOG_TAG, "Alarm triggered checking if count == 1");
		if (intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 1) <= 1) {
			Log.i(LOG_TAG, "Alarm is ok, trigger new");
			Intent newIntent = new Intent(context, AcknowledgeActivity.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			newIntent.putExtra(context.getString(R.string.countDownTimerPref), mCountdown);
			newIntent.putExtra(context.getString(R.string.phoneNumberPref), phoneNumber);
			newIntent.putExtra(context.getString(R.string.SMSContentPref), sms);
			newIntent.putExtra(context.getString(R.string.ringtonePref), mRingtone);
			newIntent.putExtra(context.getString(R.string.vibratePref), mVibrate);
			context.startActivity(newIntent);
		}else{
			Log.i(LOG_TAG, "Alarm is from the past");	
		}
	}

	/** Get the starting time in millis for the alarm as of now. Pass a number of days
	 * we wish to fer the alarm  
	 * @param dayOffset: Number of days to offset the alarm	
	 * @return Time in millis since 1970 bla bla bla
	 */
	private long getStartTime(int dayOffset) {
		Calendar cal = Calendar.getInstance();		
		cal.set(Calendar.HOUR_OF_DAY, mHourFrom);
		cal.set(Calendar.MINUTE, mMinuteFrom);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, dayOffset);		
		return cal.getTimeInMillis();
	}
	/** No more alarms as for today, reeschedule alarm for tomorrow morning!
	 * 
	 * 
	 * @param context Context that called the receiver
	 * @param intent Intent of the call
	 */
	private void rescheduleAlarm(Context context, Intent intent) {
		// cancel now,		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent mSender = PendingIntent.getBroadcast(context, LiveButton.ACKNOWLEDGE_REQUEST_CODE, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(mSender);
		// reschedule for tomorrow	
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, getStartTime(1), mInterval, mSender);
	}



	/** Check flag on wether we should start the service or not
	 * at boot time	and do so if it's set.
	 * 
	 * @param context Context that called the receiver
	 * @param intent Intent of the call
	 */
	private void startOnBoot(Context context, Intent intent) {
		if (mStartOnBoot) {
			Log.i(LOG_TAG, "Start on boot is active, scheduling alarm");
			// if startOnBoot is enabled
			// we schedule an alarm for the next interval after boot is
			// complete
			intent = new Intent(context, HeartbeatReceiver.class);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			PendingIntent mSender = PendingIntent.getBroadcast(context, LiveButton.ACKNOWLEDGE_REQUEST_CODE, intent,
					PendingIntent.FLAG_UPDATE_CURRENT );		
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, getStartTime(0), mInterval, mSender);
		}
	}

}
