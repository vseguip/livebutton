package dms.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.widget.Toast;

public class Utils {
	public final static int SMS_SENT_REQUEST_CODE = 1005;
	public final static int SMS_DELIVERED_REQUEST_CODE = 1006;
	public final static MediaPlayer mediaPlayer = new MediaPlayer();
	static public void sendSMS(final Activity activity, String phoneNumber,
			String message) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(activity,
				SMS_SENT_REQUEST_CODE, new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(activity,
				SMS_DELIVERED_REQUEST_CODE, new Intent(DELIVERED), 0);

		// ---when the SMS has been sent---
		activity.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(activity.getBaseContext(), "SMS sent",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(activity.getBaseContext(),
							"Generic failure", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(activity.getBaseContext(), "No service",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(activity.getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(activity.getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		activity.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context contect, Intent intent) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(activity.getBaseContext(), "SMS delivered",
							Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(activity.getBaseContext(),
							"SMS not delivered", Toast.LENGTH_SHORT).show();
					break;
				}
			}

		}, new IntentFilter(DELIVERED));

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	} 

	static void stopAlarm() {
		mediaPlayer.stop();
	}
	static void playAlarm(Activity activity) {
		try {
			
			//Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			mediaPlayer.reset();
			mediaPlayer.setDataSource(activity, alert);
			final AudioManager audioManager = (AudioManager) activity
					.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();				
			}
			
		} catch (Exception ex) {
			Toast.makeText(activity.getBaseContext(),
					"Could not play alarm", Toast.LENGTH_SHORT).show();
		}
	}
	public static String pad(int c) {
	    if (c >= 10)
	        return String.valueOf(c);
	    else
	        return "0" + String.valueOf(c);
	}
}
