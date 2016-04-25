package com.abdalladelessa.locmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;

import com.abdalladelessa.locmanager.providers.FuseLocationProvider;
import com.abdalladelessa.locmanager.providers.ILocationProvider;
import com.abdalladelessa.locmanager.providers.StandardLocationProvider;

import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class LocManager {
    private Handler mainHandler = new Handler();
    private ILocationProvider currentLocationProvider;
    private Timer settingsCheckerTimer;

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

    // ---------------------> Choose Best Provider

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
                    subscriber.onError(new LocException(LocUtils.ERROR_CODE_CONTEXT_IS_NULL));
                }
                else if(currentLocationProvider == null) {
                    subscriber.onError(new LocException(LocUtils.ERROR_CODE_PROVIDER_IS_NULL));
                }
                else if(!LocUtils.checkHasPlayServices(context) && currentLocationProvider instanceof FuseLocationProvider) {
                    subscriber.onError(new LocException(LocUtils.ERROR_CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND));
                }
                else {
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                }
            }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean aBoolean) {
                return LocUtils.checkHasLocationPermissions();
            }
        }).flatMap(new Func1<Boolean, Observable<Location>>() {
            @Override
            public Observable<Location> call(Boolean aBoolean) {
                return currentLocationProvider.getLocation(context);
            }
        }).compose(attachCheckLocationSettingsTimer(new Runnable() {
            @Override
            public void run() {
                if(context != null && currentLocationProvider != null && context instanceof Activity && !LocUtils.checkLocationSettingsIsEnabled(context)) {
                    currentLocationProvider.askUserToEnableLocationSettingsIfNot((Activity) context);
                }
            }
        }));
    }

    public Observable<Location> getSingleLocation(final Context context) {
        return getLocationUpdates(context).first();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(currentLocationProvider != null) {
            currentLocationProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ---------------------> Location Settings Timer

    public Observable.Transformer<Location, Location> attachCheckLocationSettingsTimer(final Runnable onSettingsNotEnabledAction) {
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
                                LocUtils.logError(e);
                            }
                        }
                    });
                }
            }
        }, LocUtils.DELAY_IN_MILLIS, LocUtils.TIMEOUT_IN_MILLIS);
    }

    private void cancelLocationSettingsTimer() {
        if(settingsCheckerTimer != null) {
            settingsCheckerTimer.cancel();
        }
    }


}
