package dms.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AcknowledgeActivity extends Activity {
	final static int CALL_REQUEST = 1003;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acknowledge);
		final String phoneNumber = getIntent().getStringExtra(LiveButton.PREF_PHONE_NUMBER);
		final String sms = getIntent().getStringExtra(LiveButton.PREF_SMS_CONTENT);
		final TextView textCounter = (TextView) findViewById(R.id.timeRemaining);
		textCounter.setText("10");
		Utils.playAlarm(this);
		final CountDownTimer timer = new CountDownTimer(10000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				textCounter.setText(Long.toString(millisUntilFinished/1000));
			}

			@Override
			public void onFinish() {
				Utils.stopAlarm();
				Intent newIntent = new Intent(AcknowledgeActivity.this, SendMessageService.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				newIntent.putExtra("phoneNumber", phoneNumber);
				newIntent.putExtra("SMS", sms);
				AcknowledgeActivity.this.startService(newIntent);
				finish();
			}
		};

		Button buttonAck = (Button) findViewById(R.id.buttonCancelCall);
		buttonAck.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				timer.cancel();
				Utils.stopAlarm();
				AcknowledgeActivity.this.finish();
			}
		});
		timer.start();
	}

}
