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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.abc.MaterialMenuIconCompat;
import com.crashlytics.android.Crashlytics;
import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Premium.PremiumFragment_;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.Settings.SettingsFragment_;
import com.doplgangr.secrecy.UpdateManager.AppVersion_;
import com.doplgangr.secrecy.UpdateManager.UpdateManager_;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.DummyViews.NavDrawer.DrawerLayout;
import com.doplgangr.secrecy.Views.DummyViews.NavDrawer.NavItem;
import com.doplgangr.secrecy.Views.DummyViews.NavDrawer.NavListView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;

@EActivity(R.layout.activity_main)
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
    @ViewById(R.id.left_drawer_list)
    NavListView mNavigation;
    @ViewById(R.id.left_drawer)
    View mDrawer;
    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    FragmentManager fragmentManager;
    MaterialMenuIconCompat materialMenu;

    @AfterViews
    public void onCreate() {
        Crashlytics.start(this);
        storage.deleteTemp();                                           //Start clean
        fragmentManager = getSupportFragmentManager();
        switchFragment(0);

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
            if (pInfo.versionCode != version.no().get())
                addFragment(new UpdateManager_(), R.anim.slide_in_right, R.anim.fadeout);
        }
        materialMenu = new MaterialMenuIconCompat(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
        mNavigation.addNavigationItem(
                CustomApp.context.getString(R.string.Page_header__vaults),
                R.drawable.ic_vault,
                false);

        mNavigation.addNavigationItem(
                CustomApp.context.getString(R.string.Page_header__settings),
                R.drawable.ic_setting,
                false);
        mNavigation.addNavigationItem(
                CustomApp.context.getString(R.string.action__donate),
                R.drawable.ic_love,
                false);
        mNavigation.addNavigationItem(
                CustomApp.context.getString(R.string.action__support),
                R.drawable.ic_help,
                false);
        mNavigation.setNavigationItemClickListener(new NavListView.NavigationItemClickListener() {
            @Override
            public void onNavigationItemSelected(String item, ArrayList<NavItem> items, int position) {
                Util.log(position, "Clicked");
                switchFragment(position);
                mDrawerLayout.closeDrawers();
            }
        });

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_launcher, 0, 0) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                materialMenu.animateState(MaterialMenuDrawable.IconState.X);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        showHelpDeskTutorial();
    }

    private void showHelpDeskTutorial() {
        if (Prefs.showHelpDeskTutorial().get())
            Util.alert(this,
                    getString(R.string.Dialog__help_centre_tutorial),
                    getString(R.string.Dialog__help_centre_tutorial_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            support();
                            Prefs.showHelpDeskTutorial()
                                    .put(false);
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //do nothing
                            Prefs.showHelpDeskTutorial()
                                    .put(false);
                        }
                    }
            );

    }

    void switchFragment(int page) {
        switch (page) {
            case 0:
                addFragment(new VaultsListFragment_(), 0, 0);
                break;
            case 1:
                addFragment(new SettingsFragment_(), 0, 0);
                break;
            case 2:
                addFragment(new PremiumFragment_(), 0, 0);
                break;
            case 3:
                support();
                return; //Do not set highlighted
        }
        mNavigation.setSelectedItem(page);
    }

    void onFirstLaunch() {
        final View dialogView = View.inflate(context, R.layout.dialog_finish_stealth, null);
        String password = Prefs.OpenPIN().get();
        ((TextView) dialogView
                .findViewById(R.id.stealth_keycode))
                .append(password);
        new AlertDialog.Builder(context)
                .setInverseBackgroundForced(true)
                .setMessage(getString(R.string.Stealth__finish))
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

    @OptionsItem(R.id.home)
    void supporthomePressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            mDrawerLayout.closeDrawer(mDrawer);
        } else {
            mDrawerLayout.openDrawer(mDrawer);
        }
    }

    @OptionsItem(android.R.id.home)
    void homePressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            mDrawerLayout.closeDrawer(mDrawer);
        }else {
            mDrawerLayout.openDrawer(mDrawer);
        }
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
                .replace(R.id.content_frame, fragment, tag)
                .addToBackStack(tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    void support() {
        Util.openURI(Config.support_website);    //launch uservoice portal
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

    @Override

    public boolean onPrepareOptionsMenu(Menu menu) {

        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen =
                mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawer);
        hideMenuItems(menu, !drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    private void hideMenuItems(Menu menu, boolean visible){
        for(int i = 0; i < menu.size(); i++)
            menu.getItem(i).setVisible(visible);
    }
}
