package dms.android;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AcknowledgeActivity extends Activity implements Runnable{
	final static int CALL_REQUEST = 1003;
	final Handler handler = new Handler();
	int countdown=10;
	String phoneNumber=""; 
	TextView textCounter;
	@Override	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acknowledge);
		phoneNumber = getIntent().getStringExtra("phoneNumber");
		textCounter = (TextView) findViewById(R.id.timeRemaining);
		textCounter.setText(Integer.toString(countdown));
		Utils.playAlarm(this);
		handler.postDelayed(this,1000);
		Button buttonAck = (Button)findViewById(R.id.buttonCancelCall);
		buttonAck.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handler.removeCallbacks(AcknowledgeActivity.this);
				Utils.stopAlarm();
				AcknowledgeActivity.this.finish();
			}
		});
	}
//	@Override	
//	public void onView(){
//		
//	}
//	
	@Override
	public void run() {
		countdown--;		
		textCounter.setText(Integer.toString(countdown));
		if(countdown == 0) {
			//TODO: call number
			countdown = 10;
			Utils.stopAlarm();			
			Utils.sendSMS(this, phoneNumber, "User stopped responding at "+ Calendar.getInstance().getTime().toLocaleString());
			finish();
//			Intent sendIntent = new Intent(Intent.ACTION_VIEW);
//			sendIntent.putExtra("sms_body", "User failed to acknowledge awareness. "); 
//			sendIntent.setType("vnd.android-dir/mms-sms");
//			startActivity(sendIntent);   
			
			//startActivityForResult(new Intent(Intent.ACTION_CALL, Uri.parse("tel:+"+phoneNumber)), CALL_REQUEST);
		}
		else{
			handler.postDelayed(this,1000);			
		}
		
	};
	
}
