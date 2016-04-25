package com.abdalladelessa.rxlocmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;

import com.abdalladelessa.rxlocmanager.providers.FuseLocationProvider;
import com.abdalladelessa.rxlocmanager.providers.ILocationProvider;
import com.abdalladelessa.rxlocmanager.providers.StandardLocationProvider;
import com.karumi.dexter.Dexter;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class RxLocManager {
    private static final boolean MOCK_LOCATION = false;//GeneralUtils.isTestMode();
    private Handler mainHandler = new Handler();
    private ILocationProvider currentLocationProvider;
    private Timer settingsCheckerTimer;

    public static void init(Context context) {
        Dexter.initialize(context);
        RxLocUtils.initResources(context);
    }

    public static RxLocManager getFuseGoogleApiBasedLocationManager() {
        return new RxLocManager(new FuseLocationProvider());
    }

    public static RxLocManager getStandardBasedLocationManager() {
        return new RxLocManager(new StandardLocationProvider());

    }

    public static RxLocManager getBestManager(Context context) {
        return new RxLocManager(context);

    }

    // --------------------->

    private RxLocManager(Context context) {
        this(getBestLocationProvider(context));
    }

    private RxLocManager(ILocationProvider currentLocationProvider) {
        this.currentLocationProvider = currentLocationProvider;
    }

    // ---------------------> Choose Best Provider

    static ILocationProvider getBestLocationProvider(Context context) {
        ILocationProvider iLocationProvider = null;
        if(context != null) {
            if(RxLocUtils.checkHasPlayServices(context)) {
                iLocationProvider = new FuseLocationProvider();
            }
            else {
                iLocationProvider = new StandardLocationProvider();
            }
        }
        return iLocationProvider;
    }

    // --------------------->

    public static Observable<List<Address>> translateLocation(final Location location) {
        return Observable.create(new Observable.OnSubscribe<List<Address>>() {
            @Override
            public void call(Subscriber<? super List<Address>> subscriber) {
                Geocoder geocoder = new Geocoder(MyApplication.getAppContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    subscriber.onNext(addresses);
                    subscriber.onCompleted();
                }
                catch(Throwable e) {
                    subscriber.onError(e);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(currentLocationProvider != null) {
            currentLocationProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Observable<Location> getLocationUpdates(final Context context) {
        if(MOCK_LOCATION) {
            Location location = new Location("");
            location.setLatitude(30.0444);
            location.setLongitude(31.2357);
            return Observable.just(location);
        }
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if(context == null) {
                    subscriber.onError(new RxLocException(RxLocUtils.ERROR_CODE_CONTEXT_IS_NULL));
                }
                else if(currentLocationProvider == null) {
                    subscriber.onError(new RxLocException(RxLocUtils.ERROR_CODE_PROVIDER_IS_NULL));
                }
                else if(!RxLocUtils.checkHasPlayServices(context) && currentLocationProvider instanceof FuseLocationProvider) {
                    subscriber.onError(new RxLocException(RxLocUtils.ERROR_CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND));
                }
                else {
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                }
            }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean aBoolean) {
                return RxLocUtils.checkHasLocationPermissions();
            }
        }).flatMap(new Func1<Boolean, Observable<Location>>() {
            @Override
            public Observable<Location> call(Boolean aBoolean) {
                return currentLocationProvider.getLocation(context);
            }
        }).compose(attachCheckLocationSettingsTimer(new Runnable() {
            @Override
            public void run() {
                if(context != null && currentLocationProvider != null && context instanceof Activity && !RxLocUtils.checkLocationSettingsIsEnabled(context)) {
                    currentLocationProvider.askUserToEnableLocationSettingsIfNot((Activity) context);
                }
            }
        })).doOnNext(new Action1<Location>() {
            @Override
            public void call(Location location) {
                RxLocUtils.log("New Location : " + location);
            }
        });
    }

    public Observable<Location> getSingleLocation(final Context context) {
        return getLocationUpdates(context).first();
    }

    // ---------------------> Location Settings Timer

    private Observable.Transformer<Location, Location> attachCheckLocationSettingsTimer(final Runnable onSettingsNotEnabledAction) {
        return new Observable.Transformer<Location, Location>() {
            @Override
            public Observable<Location> call(Observable<Location> observable) {
                return observable.doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        runLocationSettingsTimer(onSettingsNotEnabledAction);
                    }
                }).doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        cancelLocationSettingsTimer();
                    }
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelLocationSettingsTimer();
                    }
                }).doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        cancelLocationSettingsTimer();
                    }
                });
            }
        };
    }

    private void runLocationSettingsTimer(final Runnable runnable) {
        cancelLocationSettingsTimer();
        settingsCheckerTimer = new Timer();
        settingsCheckerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(mainHandler != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(runnable != null) {
                                    runnable.run();
                                }
                            }
                            catch(Exception e) {
                                RxLocUtils.logError(e);
                            }
                        }
                    });
                }
            }
        }, RxLocUtils.DELAY_IN_MILLIS, RxLocUtils.TIMEOUT_IN_MILLIS);
    }

    private void cancelLocationSettingsTimer() {
        if(settingsCheckerTimer != null) {
            settingsCheckerTimer.cancel();
        }
    }

}
