package dms.android;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HeartbeatReceiver extends BroadcastReceiver {
	// TODO: reschedule and make sure it's between hours!
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences mSettings = context.getSharedPreferences(LiveButton.PREFS_NAME, 0);
		int mHourUntil = mSettings.getInt(LiveButton.PREF_HOUR_UNTIL, 21);
		int mMinuteUntil = mSettings.getInt(LiveButton.PREF_MINUTE_UNTIL, 0);
		Date now = Calendar.getInstance().getTime();
		Date max = Calendar.getInstance().getTime();
		max.setHours(mHourUntil);
		max.setMinutes(mMinuteUntil);
		if (now.after(max)) {
			String phoneNumber = intent.getStringExtra(LiveButton.PREF_PHONE_NUMBER);
			String sms = intent.getStringExtra(LiveButton.PREF_SMS_CONTENT);			
			int mHourFrom = mSettings.getInt(LiveButton.PREF_HOUR_FROM, 9);
			int mMinuteFrom = mSettings.getInt(LiveButton.PREF_MINUTE_FROM, 0);
			long interval =  intent.getIntExtra(LiveButton.PREF_INTERVAL_MILLIS,60*60*1000);
			// cancel now, 
			AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent newIntent = new Intent(context, HeartbeatReceiver.class);
			newIntent.putExtra(LiveButton.PREF_PHONE_NUMBER, phoneNumber);
			newIntent.putExtra(LiveButton.PREF_SMS_CONTENT, sms);
			newIntent.putExtra(LiveButton.PREF_INTERVAL_MILLIS, interval);
			PendingIntent mSender = PendingIntent.getBroadcast(context, LiveButton.ACKNOWLEDGE_REQUEST_CODE, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.cancel(mSender);
			//reschedule for tomorrow
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR, mHourFrom);
			cal.set(Calendar.MINUTE, mMinuteFrom);
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), interval,
					mSender);

		} else {
			Intent newIntent = new Intent(context, AcknowledgeActivity.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			String phoneNumber = intent.getStringExtra(LiveButton.PREF_PHONE_NUMBER);
			String sms = intent.getStringExtra(LiveButton.PREF_SMS_CONTENT);
			newIntent.putExtra(LiveButton.PREF_PHONE_NUMBER, phoneNumber);
			newIntent.putExtra(LiveButton.PREF_SMS_CONTENT, sms);
			context.startActivity(newIntent);
		}
	}

}
