package dms.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
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
	public final static MediaPlayer mediaPlayer = new MediaPlayer();



	static void stopAlarm() {
		mediaPlayer.stop();
	}

	static void playAlarm(Activity activity) {
		try {

			// Uri alert =
			// RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			Uri alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
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
			Toast.makeText(activity.getBaseContext(), "Could not play alarm",
					Toast.LENGTH_SHORT).show();
		}
	}

	public static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

}
