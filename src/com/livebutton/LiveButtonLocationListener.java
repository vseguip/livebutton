package com.livebutton;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class LiveButtonLocationListener implements LocationListener {

	private static final String LOG_TAG = "LiveButtonLocationListener";
	private LocationKeeper mLocKeeper;
	private Runnable mNewFixCallback;
	private Runnable mEventCallback;

	public LiveButtonLocationListener(LocationKeeper locKeeper, Runnable newFixCallback, Runnable eventCallback) {
		Log.i(LOG_TAG, "Creating listener");
		mLocKeeper = locKeeper;
		mNewFixCallback = newFixCallback;
		mEventCallback = eventCallback;
	}

	public void onLocationChanged(Location location) {
		Log.i(LOG_TAG, "Location changed");
		if (LocationKeeper.checkAndSetLocation(location)) {
			Log.i(LOG_TAG, "More accurate location received. Calling new fix callback");
			mNewFixCallback.run(); // run new fix callback from service
		}
		Log.i(LOG_TAG, "No more locations needed, stopping them");
		mLocKeeper.stopUpdate(this);
		// run callback from service even if not new fix so we can deregister
		// and stop
		Log.i(LOG_TAG, "Run generic event callback");
		mEventCallback.run();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if ((status == LocationProvider.OUT_OF_SERVICE) || (status == LocationProvider.TEMPORARILY_UNAVAILABLE)) {
			Log.i(LOG_TAG, "Status for provider "+ provider + " changed. Run generic event callback");
			mEventCallback.run(); // run callback from service even so we can
									// deregister
		}
	}

	public void onProviderEnabled(String provider) {
		Log.i(LOG_TAG, "Provider " + provider + " enabled");
	}

	public void onProviderDisabled(String provider) {
		Log.i(LOG_TAG, "Provider "+ provider + " disabled. Run generic event callback");	
		mEventCallback.run(); // run callback from service even if not new fix
								// so we can deregister
	}

}
