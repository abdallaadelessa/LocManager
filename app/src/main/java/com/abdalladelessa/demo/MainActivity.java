package com.abdalladelessa.demo;

import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.abdalladelessa.locmanager.LocException;
import com.abdalladelessa.locmanager.LocManager;
import com.abdalladelessa.locmanager.LocUtils;

import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LocManager locManager;
    private Subscription subscribe;
    private FloatingActionButton fab;
    private TextView tvLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        locManager = LocManager.getStandardBasedLocationManager();
        fab = (FloatingActionButton) findViewById(R.id.fab);
        tvLabel = (TextView) findViewById(R.id.tvLabel);
        fab.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(subscribe != null) {
            subscribe.unsubscribe();
        }
        subscribe = locManager.getLocationUpdates(MainActivity.this).subscribe(new Action1<Location>() {
            @Override
            public void call(Location location) {
                tvLabel.setText(" Location : " + location.getLatitude() + " : " + location.getLongitude());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                String error = "Unknown error";
                if(e instanceof LocException) {
                    switch(((LocException) e).getErrorCode()) {
                        case LocUtils.ERROR_CODE_CONTEXT_IS_NULL:
                            error = "Context is null";
                            break;
                        case LocUtils.ERROR_CODE_PROVIDER_IS_NULL:
                            error = "Provider is null";
                            break;
                        case LocUtils.ERROR_CODE_GOOGLE_PLAY_SERVICE_NOT_FOUND:
                            error = "Couldn't find Google Play Service";
                            break;
                        case LocUtils.ERROR_CODE_LOCATION_PERMISSION_DENIED:
                            error = "Permission Denied";
                            break;
                    }
                }
                tvLabel.setText("Error :" + error);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(subscribe != null) {
            subscribe.unsubscribe();
        }
        super.onDestroy();
    }
}
