package dms.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HeartbeatReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		 Intent newIntent = new Intent(context, AcknowledgeActivity.class);	     
	     newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	     String phoneNumber = intent.getStringExtra("phoneNumber");
	     newIntent.putExtra("phoneNumber", phoneNumber);
	     context.startActivity(newIntent);	       
	}

}
