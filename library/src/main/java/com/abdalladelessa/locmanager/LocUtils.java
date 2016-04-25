package com.abdalladelessa.locmanager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class LocUtils {
    public static final long TIME_BETWEEN_UPDATES_IN_MILLIS = 1000 * 10;
    public static final long TIMEOUT_IN_MILLIS = TIME_BETWEEN_UPDATES_IN_MILLIS * 6;
    //---->
    public static final int CODE_CONTEXT_IS_NULL = 0;
    public static final int CODE_PROVIDER_IS_NULL = 1;
    public static final int CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND = 2;
    public static final int CODE_LOCATION_PERMISSION_DENIED = 3;
    public static final int CODE_PROVIDE_DISABLED = 4;
    public static final int CODE_TIME_OUT = 5;
    public static final int CODE_SETTINGS_CHANGE_UNAVAILABLE = 6;
    public static final int CODE_NETWORK_ERROR = 7;
    //---->
    public static String TEXT_LOCATION_SETTINGS;
    public static String TEXT_LOCATION_IS_NOT_ENABLED_MESSAGE;
    public static String TEXT_SETTINGS;
    public static String TEXT_CANCEL;
    //---->

    public static void initResources(Context context) {
        if(context == null) return;
        TEXT_LOCATION_SETTINGS = "Location Settings";
        TEXT_LOCATION_IS_NOT_ENABLED_MESSAGE = "Location is not enabled. Do you want to enable it in settings?";
        TEXT_SETTINGS = "Settings";
        TEXT_CANCEL = "Cancel";
    }

    public static void log(String message) {
        Log.d("DEBUG", message);
    }

    public static void logError(Throwable throwable) {
        Log.e("DEBUG", "Error", throwable);
    }

    // -------------------->

    public static boolean hasLocationPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkHasPlayServices(Context context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        return result == ConnectionResult.SUCCESS;
    }

    public static Observable<Boolean> checkLocationPermissions() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                Dexter.checkPermissions(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        boolean areAllPermissionsGranted = report.areAllPermissionsGranted();
                        if(areAllPermissionsGranted) {
                            subscriber.onNext(areAllPermissionsGranted);
                            subscriber.onCompleted();
                        }
                        else {
                            subscriber.onError(new LocException(CODE_LOCATION_PERMISSION_DENIED));
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        });
    }
}
