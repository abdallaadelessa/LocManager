package com.abdalladelessa.rxlocmanager;

import android.Manifest;
import android.content.Context;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.abdalladelessa.rxlocmanager.RxLocException;
import com.afollestad.materialdialogs.MaterialDialog;
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
public class RxLocUtils {
    public static final long TIME_BETWEEN_UPDATES_IN_MILLIS = 1000 * 10;
    public static final int MIN_DISTANCE_FOR_UPDATES_IN_METERS = 10;
    public static final long TIMEOUT_IN_MILLIS = 1000 * 20;
    public static final long DELAY_IN_MILLIS = 1000 * 2;
    //---->
    public static final int ERROR_CODE_CONTEXT_IS_NULL = 0;
    public static final int ERROR_CODE_PROVIDER_IS_NULL = 1;
    public static final int ERROR_CODE_CHECKING_PERMISSION_IS_RUNNING = 2;
    public static final int ERROR_CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND = 3;
    public static final int ERROR_CODE_LOCATION_PERMISSION_DENIED = 4;
    public static final int ERROR_CODE_GOOGLE_API_CONNECTION_FAILED_ERROR = 5;
    //---->
    public static String TEXT_LOCATION_SETTINGS;
    public static String TEXT_LOCATION_IS_NOT_ENABLED_MESSAGE;
    public static String TEXT_SETTINGS;
    public static String TEXT_RETRY;
    public static String TEXT_CANCEL;
    public static String ERROR_TEXT_COULDNT_RECEIVE_LOCATION;
    public static String ERROR_TEXT_COULDNT_FIND_GOOGLE_PLAY_SERVICE;
    public static String ERROR_TEXT_PERMISSION_DENIED;
    //---->

    public static void initResources(Context context) {
        if(context == null) return;
        TEXT_LOCATION_SETTINGS = "Location Settings";
        TEXT_LOCATION_IS_NOT_ENABLED_MESSAGE = "Location is not enabled. Do you want to enable it in settings?";
        TEXT_SETTINGS = "Settings";
        TEXT_RETRY = "Retry";
        TEXT_CANCEL = "Cancel";
        ERROR_TEXT_COULDNT_RECEIVE_LOCATION = "Cpuldnt retreive location";
        ERROR_TEXT_COULDNT_FIND_GOOGLE_PLAY_SERVICE = "Couldn't find Google Play Service";
        ERROR_TEXT_PERMISSION_DENIED = "Permission Denied";
    }

    public static void log(String message) {
        Log.d("DEBUG", message);
    }

    public static void logError(Throwable throwable) {
        Log.e("DEBUG", "Error", throwable);
    }

    // -------------------->

    public static boolean checkLocationSettingsIsEnabled(final Context context) {
        boolean canContinue = true;
        boolean isGPSEnabled = false;
        boolean isNetworkEnabled = false;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        catch(Exception e) {
            logError(e);
        }
        try {
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        catch(Exception e) {
            logError(e);
        }
        if(!isNetworkEnabled && !isGPSEnabled) {
            canContinue = false;
        }
        return canContinue;
    }

    public static boolean checkHasPlayServices(Context context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        return result == ConnectionResult.SUCCESS;
    }

    public static Observable<Boolean> checkHasLocationPermissions() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                if(Dexter.isRequestOngoing()) {
                    subscriber.onError(new RxLocException(ERROR_CODE_CHECKING_PERMISSION_IS_RUNNING));
                    return;
                }
                Dexter.checkPermissions(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        boolean areAllPermissionsGranted = report.areAllPermissionsGranted();
                        if(areAllPermissionsGranted) {
                            subscriber.onNext(areAllPermissionsGranted);
                            subscriber.onCompleted();
                        }
                        else {
                            subscriber.onError(new RxLocException(ERROR_CODE_LOCATION_PERMISSION_DENIED));
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

    // -------------------->

    @NonNull
    public static String getRxLocErrorMessage(Throwable e) {
        String error = ERROR_TEXT_COULDNT_RECEIVE_LOCATION;
        if(e instanceof RxLocException) {
            switch(((RxLocException) e).getErrorCode()) {
                case ERROR_CODE_CONTEXT_IS_NULL:
                    RxLocUtils.logError(new Exception("getRxLocErrorMessage Context is null"));
                    break;
                case ERROR_CODE_PROVIDER_IS_NULL:
                    RxLocUtils.logError(new Exception("getRxLocErrorMessage Current Provider is null"));
                    break;
                case ERROR_CODE_CHECKING_PERMISSION_IS_RUNNING:
                    RxLocUtils.logError(new Exception("getRxLocErrorMessage Checking Permission is Running"));
                    break;
                case ERROR_CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND:
                    error = ERROR_TEXT_COULDNT_FIND_GOOGLE_PLAY_SERVICE;
                    break;
                case ERROR_CODE_LOCATION_PERMISSION_DENIED:
                    error = ERROR_TEXT_PERMISSION_DENIED;
                    break;
            }
        }
        return error;
    }

    public static MaterialDialog showLocationErrorDialog(Context mContext, Throwable throwable, boolean cancelable, MaterialDialog.SingleButtonCallback onPositive, MaterialDialog.SingleButtonCallback onNegative) {
        if(throwable == null || throwable.getMessage() == null) return null;
        return new MaterialDialog.Builder(mContext).content(getRxLocErrorMessage(throwable)).cancelable(cancelable).positiveText(TEXT_RETRY).negativeText(TEXT_CANCEL).onPositive(onPositive).onNegative(onNegative).show();
    }
}
