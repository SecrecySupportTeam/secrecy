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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Premium.PremiumActivity_;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.Settings.SettingsActivity_;
import com.doplgangr.secrecy.UpdateManager.AppVersion_;
import com.doplgangr.secrecy.UpdateManager.UpdateManager_;
import com.doplgangr.secrecy.Util;
import com.flurry.android.FlurryAgent;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity
        extends ActionBarActivity
        implements
        VaultsListFragment.OnVaultSelectedListener,
        VaultsListFragment.OnFragmentFinishListener {
    private final Context context = this;
    @Pref
    AppVersion_ version;
    @Pref
    Prefs_ Prefs;
    FragmentManager fragmentManager;
    private Integer versionnow;
    private String versionnow_name;

    @AfterViews
    public void onCreate() {
        fragmentManager = getSupportFragmentManager();
        if (Prefs.stealthMode().get() == -1) {
            //if this is the first time, display a dialog to inform successful trial
            onFirstLaunch();
            return;
        }
        PackageInfo pInfo;
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
                addFragment(new UpdateManager_(), R.anim.slide_in_right, R.anim.fadeout);
            }
        }
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

    @Override
    public void onVaultSelected(String vault, String password) {
        Intent intent = new Intent(this, FilesActivity_.class);
        intent.putExtra(Config.vault_extra, vault);
        intent.putExtra(Config.password_extra, password);
        startActivity(intent);
    }

    void addFragment(final Fragment fragment, int transition1, int transition2) {
        String tag = fragment.getClass().getName();
        fragmentManager.beginTransaction()
                .setCustomAnimations(transition1, transition2)
                .replace(android.R.id.content, fragment, tag)
                .addToBackStack(tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @OptionsItem(R.id.action_settings)
    void settings() {
        startActivity(new Intent(context, SettingsActivity_.class));
    }

    @OptionsItem(R.id.action_donate)
    void donate() {
        startActivity(new Intent(context, PremiumActivity_.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Prefs.analytics().get())
            FlurryAgent.onStartSession(context, Config.flurryKey);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Prefs.analytics().get())
            FlurryAgent.onEndSession(context);
    }

    @Override
    public void onFinish(Fragment fragment) {
        fragmentManager.beginTransaction()
                .remove(fragment)
                .commit();
    }

    @Override
    public void onNew(Bundle bundle, Fragment fragment) {
        fragment.setArguments(bundle);
        addFragment(fragment, R.anim.slide_in_right, R.anim.fadeout);
    }

    @Override
    public void onDestroy() {
        storage.deleteTemp(); //Cleanup every time
        super.onDestroy();
    }
}
