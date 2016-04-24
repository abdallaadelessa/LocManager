package com.abdalladelessa.locmanager.providers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import com.abdalladelessa.locmanager.LocException;
import com.abdalladelessa.locmanager.LocUtils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class StandardLocationProvider implements ILocationProvider {
    public static final int MIN_DISTANCE_FOR_UPDATES_IN_METERS = 10;
    private static final int ACCURACY = Criteria.ACCURACY_FINE;
    private static final int POWER = Criteria.POWER_LOW;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Criteria criteria;
    private String currentProvider;

    // ------------------->

    @Override
    public Observable<Location> getLocation(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Location>() {
            @Override
            public void call(final Subscriber<? super Location> subscriber) {
                try {
                    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if(checkProvidersIsEnabled(context)) {
                        criteria = new Criteria();
                        criteria.setAccuracy(ACCURACY);
                        criteria.setPowerRequirement(POWER);
                        criteria.setAltitudeRequired(false);
                        criteria.setBearingRequired(false);
                        currentProvider = locationManager.getBestProvider(criteria, true);
                        LocUtils.log("CurrentProvider : " + currentProvider);
                        locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                subscriber.onNext(location);
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {

                            }

                            @Override
                            public void onProviderEnabled(String provider) {

                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                                if(currentProvider.equals(provider)) {
                                    Observable.error(new LocException(LocUtils.CODE_PROVIDE_DISABLED));
                                }
                            }
                        };
                        locationManager.requestLocationUpdates(currentProvider, LocUtils.TIME_BETWEEN_UPDATES_IN_MILLIS, MIN_DISTANCE_FOR_UPDATES_IN_METERS, locationListener);
                        // Last Known Location
                        Location location = getLastLocation(context);
                        if(location != null) {
                            subscriber.onNext(location);
                        }
                    }
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

    private void disconnect() {
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

    // ------------------->

    private boolean checkProvidersIsEnabled(final Context context) {
        boolean canContinue = true;
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isNetworkEnabled || !isGPSEnabled) {
            canContinue = false;
            showLocationNotEnabledDialog(context);
        }
        return canContinue;
    }

    private void showLocationNotEnabledDialog(final Context context) {
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

    // ------------------->

    private Location getLastLocation(Context context) {
        Location gpsLocation = getLastLocationByProvider(context, LocationManager.GPS_PROVIDER);
        Location networkLocation = getLastLocationByProvider(context, LocationManager.NETWORK_PROVIDER);
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

    private Location getLastLocationByProvider(Context context, String provider) {
        Location location = null;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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
