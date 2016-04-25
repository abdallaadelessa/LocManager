package com.abdalladelessa.rxlocmanager;

import android.app.Application;

import com.karumi.dexter.Dexter;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Dexter.initialize(this);
    }
}
