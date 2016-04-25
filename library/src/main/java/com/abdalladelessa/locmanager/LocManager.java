package com.abdalladelessa.locmanager;

import android.content.Context;
import android.location.Location;

import com.abdalladelessa.locmanager.providers.FuseLocationProvider;
import com.abdalladelessa.locmanager.providers.ILocationProvider;
import com.abdalladelessa.locmanager.providers.StandardLocationProvider;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class LocManager {
    private ILocationProvider currentLocationProvider;

    public static LocManager getFuseGoogleApiBasedLocationManager() {
        return new LocManager(new FuseLocationProvider());
    }

    public static LocManager getStandardBasedLocationManager() {
        return new LocManager(new StandardLocationProvider());

    }

    public static LocManager getBestManager(Context context) {
        return new LocManager(context);

    }

    // --------------------->

    private LocManager(Context context) {
        this(getBestLocationProvider(context));
    }

    private LocManager(ILocationProvider currentLocationProvider) {
        this.currentLocationProvider = currentLocationProvider;
    }

    // --------------------->

    static ILocationProvider getBestLocationProvider(Context context) {
        ILocationProvider iLocationProvider = null;
        if(context != null) {
            if(LocUtils.checkHasPlayServices(context)) {
                iLocationProvider = new FuseLocationProvider();
            }
            else {
                iLocationProvider = new StandardLocationProvider();
            }
        }
        return iLocationProvider;
    }

    // --------------------->

    public Observable<Location> getLocationUpdates(final Context context) {
        LocUtils.initResources(context);

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if(context == null) {
                    subscriber.onError(new LocException(LocUtils.CODE_CONTEXT_IS_NULL));
                }
                else if(currentLocationProvider == null) {
                    subscriber.onError(new LocException(LocUtils.CODE_PROVIDER_IS_NULL));
                }
                else if(!LocUtils.checkHasPlayServices(context) && currentLocationProvider instanceof FuseLocationProvider) {
                    subscriber.onError(new LocException(LocUtils.CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND));
                }
                else {
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                }
            }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean aBoolean) {
                return LocUtils.checkLocationPermissions();
            }
        }).flatMap(new Func1<Boolean, Observable<Location>>() {
            @Override
            public Observable<Location> call(Boolean aBoolean) {
                return currentLocationProvider.getLocation(context);
            }
        });
    }

    public Observable<Location> getSingleLocation(final Context context) {
        return getLocationUpdates(context).first();
    }

    public Observable<Location> getSingleLocationWithTimeOut(final Context context) {
        final Observable<Location> timeoutObservable = Observable.error(new LocException(LocUtils.CODE_TIME_OUT));
        return getSingleLocation(context).timeout(LocUtils.TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS, timeoutObservable, AndroidSchedulers.mainThread());
    }
}
