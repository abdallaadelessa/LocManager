package com.abdalladelessa.locmanager.providers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.abdalladelessa.locmanager.RxLocException;
import com.abdalladelessa.locmanager.RxLocUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class FuseLocationProvider implements ILocationProvider {
    public static final int REQUEST_CHECK_LOCATION_SETTINGS = 787;
    public static final long FASTEST_INTERVAL = 1000 * 5;
    private static final int PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private boolean isDialogShown;

    // ------------------->

    public Observable<Location> getLocation(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Location>() {
            @Override
            public void call(final Subscriber<? super Location> subscriber) {
                try {
                    locationRequest = new LocationRequest();
                    locationRequest.setInterval(RxLocUtils.TIME_BETWEEN_UPDATES_IN_MILLIS);
                    locationRequest.setFastestInterval(FASTEST_INTERVAL);
                    locationRequest.setPriority(PRIORITY);
                    googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            try {
                                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        subscriber.onNext(location);
                                    }
                                });
                                Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                                if(lastLocation != null) {
                                    subscriber.onNext(lastLocation);
                                }
                            }
                            catch(SecurityException e) {
                                subscriber.onError(e);
                            }
                            catch(Throwable e) {
                                subscriber.onError(e);
                            }
                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            RxLocUtils.log(connectionResult.getErrorMessage());
                            subscriber.onError(new RxLocException(RxLocUtils.ERROR_CODE_GOOGLE_API_CONNECTION_FAILED_ERROR));
                        }
                    }).build();
                    googleApiClient.connect();
                }
                catch(Throwable e) {
                    subscriber.onError(e);
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
        if(isDialogShown) return;
        final LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true).build();
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, settingsRequest).setResultCallback(new ResultCallback<Result>() {
            @Override
            public void onResult(@NonNull Result result) {
                try {
                    final Status status = result.getStatus();
                    switch(status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                RxLocUtils.log("LocationSettingsStatusCodes RESOLUTION_REQUIRED");
                                status.startResolutionForResult(context, REQUEST_CHECK_LOCATION_SETTINGS);
                                isDialogShown = true;
                            }
                            catch(IntentSender.SendIntentException e) {
                                RxLocUtils.logError(e);
                            }
                            break;
                    }
                }
                catch(Throwable e) {
                    RxLocUtils.logError(e);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            isDialogShown = false;
        }
    }

    public void disconnect() {
        try {
            if(googleApiClient != null) {
                googleApiClient.disconnect();
            }
        }
        catch(Throwable e) {
            RxLocUtils.logError(e);
        }
    }

    // ------------------->
}
