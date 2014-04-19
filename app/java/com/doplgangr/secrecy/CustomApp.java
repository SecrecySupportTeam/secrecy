package com.doplgangr.secrecy;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.androidannotations.annotations.EApplication;

/**
 * Created by Matthew on 4/5/2014.
 */
@EApplication
public class CustomApp extends Application {
    public static Context context;
    public static String VERSIONNAME = "";
    public static int VERSIONCODE = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            VERSIONNAME = pInfo.versionName;
            VERSIONCODE = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
