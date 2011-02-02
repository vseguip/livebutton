package dms.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AcknowledgeActivity extends Activity implements Runnable {
	final static int CALL_REQUEST = 1003;
	final Handler handler = new Handler();
	int countdown = 10;
	String phoneNumber = "";
	String sms = "";
	TextView textCounter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acknowledge);
		phoneNumber = getIntent().getStringExtra(LiveButton.PREF_PHONE_NUMBER);
		sms = getIntent().getStringExtra(LiveButton.PREF_SMS_CONTENT);
		textCounter = (TextView) findViewById(R.id.timeRemaining);
		textCounter.setText(Integer.toString(countdown));
		Utils.playAlarm(this);
		handler.postDelayed(this, 1000);
		Button buttonAck = (Button) findViewById(R.id.buttonCancelCall);
		buttonAck.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.removeCallbacks(AcknowledgeActivity.this);
				Utils.stopAlarm();
				AcknowledgeActivity.this.finish();
			}
		});
	}

	@Override
	public void run() {
		countdown--;
		textCounter.setText(Integer.toString(countdown));
		if (countdown == 0) {			
			countdown = 10;
			Utils.stopAlarm();
			Intent newIntent = new Intent(this, SendMessageService.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			newIntent.putExtra("phoneNumber", phoneNumber);
			newIntent.putExtra("SMS", sms);
			this.startService(newIntent);
			finish();
			
			
		} else {
			handler.postDelayed(this, 1000);
		}

	};

}
