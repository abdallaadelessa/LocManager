package com.abdalladelessa.rxlocmanager.providers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import rx.Observable;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public interface ILocationProvider {
    Observable<Location> getLocation(Context context);

    void askUserToEnableLocationSettingsIfNot(final Activity context);

    void onActivityResult(int requestCode, int resultCode, Intent data);

    void disconnect();
}
