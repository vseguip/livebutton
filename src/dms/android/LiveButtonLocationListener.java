package dms.android;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LiveButtonLocationListener implements LocationListener {
	// context and location manager used to register the listener
	private Activity mContext;
	private LocationManager mLocationManager;
	// Access through getter function
	private static Location bestKnownLocation;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix and sets current location fix to the new one. Synchronized
	 * to protect concurrent access
	 * 
	 * @param location The new Location that you want to evaluate
	 * @return Returns wether the new location was better or not
	 */
	public static synchronized boolean checkAndSetLocation(Location newLocation) {
		if (isBetterLocation(newLocation, bestKnownLocation)) {
			bestKnownLocation = newLocation;
			return true;
		}
		return false;
	}

	

	/**
	 * Get's last known location. Synchronized to protect concurrent access.
	 * 
	 */
	public static synchronized Location getCurrentLocation() {
		return bestKnownLocation;
	}

	/**
	 * COPIED FROM GOOGLE ANDROID DOCS! Determines whether one Location reading
	 * is better than the current Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected static boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}


	public LiveButtonLocationListener(Activity context,
			LocationManager locationManager) {
		mContext = context;
		mLocationManager = locationManager;
		Location locNetwork = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location locGPS = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		checkAndSetLocation(locNetwork);
		checkAndSetLocation(locGPS);
	}

	public void onLocationChanged(Location location) {
		if (checkAndSetLocation(location)) {
			// Utils.sendSMS(mContext, phoneNumber, message);
			mLocationManager.removeUpdates(this);
		}
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}

}
