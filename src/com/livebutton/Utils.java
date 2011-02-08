package com.livebutton;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class Utils {
	private static final String LOG_TAG = "Utility";
	public final static MediaPlayer mediaPlayer = new MediaPlayer();

	static void stopAlarm() {
		mediaPlayer.stop();
	}

	static void playAlarm(Activity activity, String ringtone) {
		try {

			// Uri alert =
			// RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			Log.i(LOG_TAG, "Playing alarm: " + ringtone);
			Uri alert;
			try {
				if (ringtone == null)
					alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
				else
					alert = Uri.parse(ringtone);
				mediaPlayer.reset();
				mediaPlayer.setDataSource(activity, alert);
			} catch (Exception ex) {
				// some error on the ringtone occurred, try with the defautl one
				Log.e(LOG_TAG, Log.getStackTraceString(ex));
				alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
				mediaPlayer.reset();
				mediaPlayer.setDataSource(activity, alert);
			}
			final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();
			}

		} catch (Exception ex) {
			Log.e(LOG_TAG, Log.getStackTraceString(ex));
			Toast.makeText(activity.getBaseContext(), "Could not play alarm", Toast.LENGTH_SHORT).show();
		}
	}

	public static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

}
