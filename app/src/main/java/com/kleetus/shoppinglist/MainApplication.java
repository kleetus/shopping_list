package com.kleetus.shoppinglist;

import android.app.Application;

public class MainApplication extends Application {

    public static final Boolean dev_mode = false;
    public static String server;

    @Override
    public void onCreate() {
        if(dev_mode) {
            server = Constants.dev_host;
        }
        else {
            server = Constants.production_host;
        }
    }


}
