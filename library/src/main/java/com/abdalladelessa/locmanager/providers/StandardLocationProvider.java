package com.abdalladelessa.locmanager.providers;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import com.abdalladelessa.locmanager.LocUtils;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class StandardLocationProvider implements ILocationProvider {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public static final int MIN_DISTANCE_FOR_UPDATES_IN_METERS = 10;
    private Location lastLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;

    // ------------------->

    @Override
    public Observable<Location> getLocation(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Location>() {
            @Override
            public void call(final Subscriber<? super Location> subscriber) {
                try {
                    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    lastLocation = null;
                    locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            doUpdateLocation(location, subscriber);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            LocUtils.log("onProviderEnabled : " + provider);
                            Location location = getLastLocation();
                            doUpdateLocation(location, subscriber);
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            LocUtils.log("onProviderDisabled : " + provider);
                        }
                    };
                    for(String provider : locationManager.getAllProviders()) {
                        locationManager.requestLocationUpdates(provider, LocUtils.TIME_BETWEEN_UPDATES_IN_MILLIS, MIN_DISTANCE_FOR_UPDATES_IN_METERS, locationListener);
                    }
                    Location location = getLastLocation();
                    doUpdateLocation(location, subscriber);
                }
                catch(SecurityException e) {
                    Observable.error(e);
                }
                catch(Throwable e) {
                    Observable.error(e);
                }
            }
        }).doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                disconnect();
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                disconnect();
            }
        });
    }

    public void askUserToEnableLocationSettingsIfNot(final Activity context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(LocUtils.TEXT_LOCATION_SETTINGS);
        alertDialog.setMessage(LocUtils.TEXT_LOCATION_IS_NOT_ENABLED_MESSAGE);
        alertDialog.setPositiveButton(LocUtils.TEXT_SETTINGS, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
                dialog.cancel();
            }
        });
        alertDialog.setNegativeButton(LocUtils.TEXT_CANCEL, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    public void disconnect() {
        try {
            if(locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
        }
        catch(SecurityException e) {
            LocUtils.logError(e);
        }
        catch(Throwable e) {
            LocUtils.logError(e);
        }
    }

    // -------------------> Compare Locations

    private void doUpdateLocation(Location location, Subscriber<? super Location> subscriber) {
        LocUtils.log("New Location : " + location);
        boolean isBetterLocation = false;
        try {
            isBetterLocation = isBetterLocation(location, lastLocation);
        }
        catch(Throwable e) {
            LocUtils.logError(e);
        }
        if(isBetterLocation) {
            lastLocation = location;
            LocUtils.log("doUpdateLocation : " + lastLocation.getProvider());
            subscriber.onNext(lastLocation);
        }

    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if(location == null) {
            return false;
        }

        if(currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if(isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if(isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if(isMoreAccurate) {
            return true;
        }
        else if(isNewer && !isLessAccurate) {
            return true;
        }
        else if(isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if(provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    // -------------------> Last Known Location

    private Location getLastLocation() {
        Location gpsLocation = getLastLocationByProvider(LocationManager.GPS_PROVIDER);
        Location networkLocation = getLastLocationByProvider(LocationManager.NETWORK_PROVIDER);
        // if we have only one location available, the choice is easy
        if(gpsLocation == null && networkLocation == null) {
            return null;
        }

        if(gpsLocation == null) {
            LocUtils.log("No GPS Location available.");
            return networkLocation;
        }
        if(networkLocation == null) {
            LocUtils.log("No Network Location available");
            return gpsLocation;
        }
        // both are old return the newer of those two
        if(gpsLocation.getTime() > networkLocation.getTime()) {
            LocUtils.log("Both are old, returning gps(newer)");
            return gpsLocation;
        }
        else {
            LocUtils.log("Both are old, returning network(newer)");
            return networkLocation;
        }
    }

    private Location getLastLocationByProvider(String provider) {
        Location location = null;
        try {
            if(locationManager.isProviderEnabled(provider)) {
                location = locationManager.getLastKnownLocation(provider);
            }
        }
        catch(IllegalArgumentException | SecurityException e) {
            LocUtils.logError(e);
        }
        return location;
    }


}
