/**
 * 
 */
package dms.android;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

/**
 * @author vseguip
 * 
 * 
 *         This service is the used to send messages to the user. It will create
 *         three threads. The first one will get the last known location and
 *         send it. The other 2 ones will try to get a recent location from the
 *         Network and GPS providers and send it. As a result, up to 3 SMS may
 *         be sent.
 * 
 */
public class SendMessageService extends Service {
	private static final String LOG_TAG = "SendMessageService";
	public final static int SMS_SENT_REQUEST_CODE = 1005;
	public final static int SMS_DELIVERED_REQUEST_CODE = 1006;
	public int mNProviders;
	private final IBinder mBinder = new LocalBinder();
	private BroadcastReceiver mSMSReceiver;
	private BroadcastReceiver mSMSDelivered;
	private List<LocationKeeper> mLocKeepers;

	public class LocalBinder extends Binder {
		SendMessageService getService() {
			return SendMessageService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG_TAG, "Creating SendMessageService");
	}

	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "Destroying SendMessageService");
		Log.i(LOG_TAG, "Unregistering sms receives");
		// unregister receives on destruction
		unregisterReceiver(mSMSReceiver);
		unregisterReceiver(mSMSDelivered);
		Log.i(LOG_TAG, "Unregistering location manager");
		// unregister location managers et. al.
		for (LocationKeeper k : mLocKeepers) {
			k.stopUpdate();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int StartId) {
		try {
			Log.i(LOG_TAG, "Starting SendMessageService");
			final String phoneNumber = intent.getStringExtra("phoneNumber");
			final String sms = intent.getStringExtra("SMS");

			// Get a list of location keepers (will also update our last known
			// location
			Log.i(LOG_TAG, "Getting list of all LocationKeepers");
			final List<LocationKeeper> locs = LocationKeeper.MakeLocationKeepers(this);
			mLocKeepers = locs;
			// get best current location and send it immediately
			Log.i(LOG_TAG, "Getting best current location");
			Location loc = LocationKeeper.getCurrentLocation();
			Log.i(LOG_TAG, "Sending current best location");
			sendMessage(phoneNumber, sms, loc);
			// start a listener for each of them
			// will deregister updates for each provider once
			// it gets the first fix and then send an SMS if it's a better
			// fix than the current one
			// Can use two runnables, one for all cases (received a fix of
			// sorts)
			// and another for when the fix is better than the current
			Log.i(LOG_TAG, "Start listeners fot the rest of fixes");
			mNProviders = locs.size();
			for (final LocationKeeper k : locs) {
				k.startUpdate(new Runnable() {
					public void run() {
						Log.i(LOG_TAG, "Better location received. Sending");
						Location loc = LocationKeeper.getCurrentLocation();
						sendMessage(phoneNumber, sms, loc);						
					}
				}, new Runnable() {
					public void run() {
						Log.i(LOG_TAG, "Removing LocationKeeper");
						locs.remove(k);
						if (locs.size() == 0)
							stopSelf();
					}
				});
			}

		} catch (Exception e) {
			Log.i(LOG_TAG, Log.getStackTraceString(e));
		}
		return START_REDELIVER_INTENT;
	}

	private void sendMessage(String phoneNumber, String sms, Location loc) {
		Log.i(LOG_TAG, "Sending message to "+ phoneNumber);
		String descLoc = "(unknown position)";
		if (loc != null) {
			descLoc = // TODO: translate to real address
			"(" + loc.getProvider() + ": " + Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES) + "º, "
					+ Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES) + "º)";

		}
		if (sms == null)
			sms = getString(R.string.SMSContent);

		String formattedSms = sms.replaceAll("#position", descLoc).replaceAll("#time",
				Calendar.getInstance().getTime().toLocaleString());
		// List all providers:
		sendSMS(phoneNumber, formattedSms);
	}

	public void sendSMS(String phoneNumber, String message) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		mSMSReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Log.i(LOG_TAG, "SMS sent ");
					Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();

					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Log.e(LOG_TAG, "Generic failure");
					Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Log.e(LOG_TAG, "No service");
					Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Log.e(LOG_TAG, "Null PDU");
					Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Log.e(LOG_TAG, "Radio off");
					Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};
		// ---when the SMS has been sent---
		registerReceiver(mSMSReceiver, new IntentFilter(SENT));
		mSMSDelivered = new BroadcastReceiver() {
			@Override
			public void onReceive(Context contect, Intent intent) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Log.i(LOG_TAG, "SMS delivered");
					Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Log.e(LOG_TAG, "SMS not delivered");
					Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
					break;
				}
			}

		};
		// ---when the SMS has been delivered---
		Log.i(LOG_TAG, "Register SMS delivered listener");
		registerReceiver(mSMSDelivered, new IntentFilter(DELIVERED));
		// partir el mensaje y enviar en trozos ya que algunos telefonos
		// fallan al enviar mensajes algo largos!
		Log.i(LOG_TAG, "Partitioning message in multiple pieces");
		SmsManager sms = SmsManager.getDefault();
		ArrayList<String> mensajes = sms.divideMessage(message);
		ArrayList<PendingIntent> sentPI = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPI = new ArrayList<PendingIntent>();
		for (int i=0; i < mensajes.size(); i++) {
			sentPI.add(PendingIntent.getBroadcast(this, SMS_SENT_REQUEST_CODE, new Intent(SENT), 0));
			deliveredPI.add(PendingIntent.getBroadcast(this, SMS_DELIVERED_REQUEST_CODE, new Intent(DELIVERED), 0));
		}
		Log.i(LOG_TAG, "Send multipart SMS");
		sms.sendMultipartTextMessage(phoneNumber, null, mensajes, sentPI, deliveredPI);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
