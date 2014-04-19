package com.doplgangr.secrecy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.doplgangr.secrecy.UpdateManager.AppVersion_;
import com.doplgangr.secrecy.UpdateManager.UpdateManager_;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity
public class MainActicity extends Activity {
    @Pref
    AppVersion_ version;
    Integer versionnow;
    String versionnow_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, ListVaultActivity_.class));
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Util.log("Cannot get package info, abort.");
            return;
        }
        if (pInfo != null) {
            versionnow = pInfo.versionCode;
            versionnow_name = pInfo.versionName;
            if (versionnow != version.no().get()) {
                Intent intent = new Intent(this, UpdateManager_.class);
                startActivity(intent);
            }
        }
        finish();
    }
}
