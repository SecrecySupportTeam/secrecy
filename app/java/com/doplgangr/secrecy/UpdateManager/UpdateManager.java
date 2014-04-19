package com.doplgangr.secrecy.UpdateManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.storage;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity(R.layout.activity_update_manager)
public class UpdateManager extends Activity {
    @ViewById(R.id.log)
    TextView log;
    @ViewById(R.id.continueButton)
    Button continueButton;
    @Pref
    AppVersion_ version;
    Integer versionnow;
    String versionnow_name;

    @AfterViews
    void onCreate() {
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Util.log("Cannot get package info, abort.");
            finish();
            return;
        }
        versionnow = pInfo.versionCode;
        versionnow_name = pInfo.versionName;
        if (versionnow ==  version.no().get()) {
            finish();
            return;
        }
        appendlog(String.format(getString(R.string.previous_version), version.no().get(), version.name().get()));
        appendlog(String.format(getString(R.string.upgrade_to), (int) versionnow, versionnow_name));
        switch (version.no().get()) {
            case 5:
                version5to6();
                break;
            case 3:
                version3to5();
                break;
            case 2:
                version2to3();
                break;
            case 1:
                version1to2();
                break;
        }
        appendlog(getString(R.string.updating));
    }
    @Background
    void version5to6(){
        //Nothing to upgrade
        onFinishAllUpgrade();
    }
    @Background
    void version3to5(){
        //Nothing to upgrade
        version5to6();
    }
    @Background
    void version2to3(){
        //Nothing to upgrade
        version3to5();
    }

    @Background
    void version1to2() {
        java.io.File root = storage.getRoot();
        java.io.File[] files = root.listFiles();
        for (java.io.File inFile : files)
            if (inFile.isDirectory())
                if ("TEMP".equals(inFile.getName()))
                    storage.DeleteRecursive(inFile);
        appendlog(getString(R.string.one_to_two));
        version2to3();
    }

    @UiThread
    void onFinishAllUpgrade() {
        appendlog(getString(R.string.update_finish));
        continueButton.setEnabled(true);
        version.edit()
                .no()
                .put(versionnow)
                .name()
                .put(versionnow_name)
                .apply();
    }

    @UiThread
    void appendlog(String message) {
        log.append(message);
    }

    public void Continue(View view) {
        if (versionnow<100)
            Util.alert(this,
                    getString(R.string.alpha_header),
                    getString(R.string.alpha_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    },
            null);
        else if (versionnow<200)
            Util.alert(this,
                    getString(R.string.beta_title),
                    getString(R.string.beta_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    },
                    null);
        else
            finish();
    }
}