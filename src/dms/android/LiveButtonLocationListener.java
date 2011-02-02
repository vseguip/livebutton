package dms.android;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;

public class LiveButtonLocationListener implements LocationListener {

	private LocationKeeper mLocKeeper;
	private Runnable mNewFixCallback;
	private Runnable mEventCallback;
	public LiveButtonLocationListener(LocationKeeper locKeeper, Runnable newFixCallback, Runnable eventCallback) {
		mLocKeeper=locKeeper;
		mNewFixCallback = newFixCallback;
		mEventCallback = eventCallback;
	}
	public void onLocationChanged(Location location) {	
		if (LocationKeeper.checkAndSetLocation(location)) {					
			mNewFixCallback.run(); //run new fix callback from service
		}	
		mLocKeeper.stopUpdate(this);
		//run callback from service even if not new fix so we can deregister and stop
		mEventCallback.run(); 
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if((status == LocationProvider.OUT_OF_SERVICE) || (status==LocationProvider.TEMPORARILY_UNAVAILABLE)) 
			mEventCallback.run(); //run callback from service even so we can deregister
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
		mEventCallback.run(); //run callback from service even if not new fix so we can deregister
	}

}
