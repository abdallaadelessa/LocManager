package com.abdalladelessa.locmanager.providers;

import android.content.Context;
import android.location.Location;

import rx.Observable;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public interface ILocationProvider {
    Observable<Location> getLocation(Context context);
}
