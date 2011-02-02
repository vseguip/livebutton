package dms.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HeartbeatReceiver extends BroadcastReceiver {
	// TODO: reschedule and make sure it's between hours!
	@Override
	public void onReceive(Context context, Intent intent) {

		Intent newIntent = new Intent(context, AcknowledgeActivity.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String phoneNumber = intent
				.getStringExtra(LiveButton.PREF_PHONE_NUMBER);
		String sms = intent.getStringExtra(LiveButton.PREF_SMS_CONTENT);
		newIntent.putExtra(LiveButton.PREF_PHONE_NUMBER, phoneNumber);
		newIntent.putExtra(LiveButton.PREF_SMS_CONTENT, sms);
		context.startActivity(newIntent);
	}

}
