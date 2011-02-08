package dms.android;

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

public class HeartbeatReceiver extends BroadcastReceiver {	
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

	private void readSettings(Context context) {
		// Rescue saved preferences
		// Time
		if(mSettings==null)
			mSettings = PreferenceManager.getDefaultSharedPreferences(context);
		int hours = mSettings.getInt(context.getString(R.string.hoursRepeatPref), 0);
		int minutes = mSettings.getInt(context.getString(R.string.minutesRepeatPref), 0);
		mInterval = (context.getResources().getIntArray(R.array.hourArray)[hours] * 60)
				+ context.getResources().getIntArray(R.array.minuteArray)[minutes];
		mInterval *= 60 * 1000;// to millis
		// Start time
		mHourFrom = mSettings.getInt(context.getString(R.string.hourFromPref), 9);
		mMinuteFrom = mSettings.getInt(context.getString(R.string.minuteFromPref), 0);
		// Stop time
		mHourUntil = mSettings.getInt(context.getString(R.string.hourUntilPref), 21);
		mMinuteUntil = mSettings.getInt(context.getString(R.string.minuteUntilPref), 0);
		// countdown timer & ringtone
		//parse string because Android API doesn't allow for integer arrays to be used as preferences. Duh!
		mCountdown = Integer.parseInt(mSettings.getString(context.getString(R.string.countDownTimerPref),"10"));	
		mRingtone =  mSettings.getString(context.getString(R.string.ringtonePref), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
		// call details
		phoneNumber = mSettings.getString(context.getString(R.string.phoneNumberPref), "");
		sms = mSettings.getString(context.getString(R.string.SMSContentPref), context.getString(R.string.SMSContent));
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		readSettings(context);
		Date now = Calendar.getInstance().getTime();
		Date max = Calendar.getInstance().getTime();
		max.setHours(mHourUntil);
		max.setMinutes(mMinuteUntil);
		if (now.after(max)) {
			// cancel now,
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			PendingIntent mSender = PendingIntent.getBroadcast(context, LiveButton.ACKNOWLEDGE_REQUEST_CODE, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.cancel(mSender);
			// reschedule for tomorrow
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR, mHourFrom);
			cal.set(Calendar.MINUTE, mMinuteFrom);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), mInterval, mSender);

		} else {

			Intent newIntent = new Intent(context, AcknowledgeActivity.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			newIntent.putExtra(context.getString(R.string.countDownTimerPref), mCountdown);
			newIntent.putExtra(context.getString(R.string.phoneNumberPref), phoneNumber);
			newIntent.putExtra(context.getString(R.string.SMSContentPref), sms);
			newIntent.putExtra(context.getString(R.string.ringtonePref), mRingtone);
			context.startActivity(newIntent);
		}
	}

}
