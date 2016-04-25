package com.abdalladelessa.rxlocmanager;

import android.app.Application;
import android.content.Context;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class MyApplication extends Application {
    private static MyApplication mInstance;
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mAppContext = this;
        RxLocManager.init(this);
    }

    public static MyApplication getInstance() {
        return mInstance;
    }

    public static Context getAppContext() {
        return mAppContext;
    }
}
