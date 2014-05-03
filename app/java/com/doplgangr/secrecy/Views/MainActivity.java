/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.IntentCompat;
import android.view.View;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.UpdateManager.AppVersion_;
import com.doplgangr.secrecy.UpdateManager.UpdateManager_;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity
public class MainActivity extends Activity {
    @Pref
    AppVersion_ version;
    Integer versionnow;
    String versionnow_name;
    @Pref
    Prefs_ Prefs;
    Context context = this;

    @AfterInject
    public void onCreate() {
        if (Prefs.stealthMode().get() == -1) {
            //if this is the first time, display a dialog to inform successful trial
            onFirstLaunch();
            return;
        }
        Intent mainIntent = new Intent(this, ListVaultActivity_.class);
        mainIntent.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
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

    void onFirstLaunch() {
        final View dialogView = View.inflate(context, R.layout.dialog_finish_stealth, null);
        String password = Prefs.OpenPIN().get();
        ((TextView) dialogView
                .findViewById(R.id.password))
                .append(password);
        new AlertDialog.Builder(context)
                .setInverseBackgroundForced(true)
                .setMessage("You're all set and ready to go! We will now hide the app icon. It will be gone after you reboot. Again, remember your secret code:")
                .setView(dialogView)
                .setPositiveButton(getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Prefs.stealthMode().put(1);
                                onCreate();
                            }
                        }
                )
                .show();
    }
}
