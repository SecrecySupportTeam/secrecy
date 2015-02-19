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
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.Encryption.VaultHolder;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Premium.PremiumFragment_;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.SettingsFragment;
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
import java.util.List;

@EActivity(R.layout.activity_main)
public class MainActivity
        extends ActionBarActivity
        implements
        VaultsListFragment.OnVaultSelectedListener,
        VaultsListFragment.OnFragmentFinishListener {
    private final List<Class> mFragmentNameList = new ArrayList<Class>() {{
        add(VaultsListFragment_.class);
        add(SettingsFragment.class);
        add(PremiumFragment_.class);
    }};
    private final Context context = this;
    @Pref
    AppVersion_ version;
    @ViewById(R.id.left_drawer_list)
    NavListView mNavigation;
    @ViewById(R.id.left_drawer)
    View mDrawer;
    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @ViewById(R.id.toolbar)
    Toolbar mToolbar;
    private FragmentManager fragmentManager;
    private ActionBarDrawerToggle mDrawerToggle;

    @AfterViews
    public void onCreate() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Storage.deleteTemp();                                           //Start clean
        VaultHolder.getInstance().clear();
        fragmentManager = getSupportFragmentManager();
        switchFragment(0);
        setSupportActionBar(mToolbar);
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("showStealthModeTutorial", false)) {
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

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, 0, 0) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        Util.loadSelectedImageSize(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("image_size", "1"));
        showHelpDeskTutorial();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    private void showHelpDeskTutorial() {
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("showHelpDeskTutorial", true)) {
            Util.alert(this,
                    getString(R.string.Dialog__help_centre_tutorial),
                    getString(R.string.Dialog__help_centre_tutorial_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            support();
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }
            );

            SharedPreferences.Editor editor
                    = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean("showHelpDeskTutorial", false);
            editor.apply();
        }
    }

    void switchFragment(int page) {
        switch (page) {
            case 0:
                addFragment(new VaultsListFragment_(), 0, 0);
                break;
            case 1:
                addFragment(new SettingsFragment(), 0, 0);
                break;
            case 2:
                addFragment(new PremiumFragment_(), 0, 0);
                break;
            case 3:
                support();
                break;
        }
    }

    void onFirstLaunch() {
        final View dialogView = View.inflate(context, R.layout.dialog_finish_stealth, null);
        String password = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("stealth_mode_password", "");
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
                                SharedPreferences.Editor editor
                                        = PreferenceManager.getDefaultSharedPreferences(context).edit();
                                editor.putBoolean("showStealthModeTutorial", false);
                                editor.apply();
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
        } else {
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
        if (mFragmentNameList.contains(fragment.getClass()))
            mNavigation.setSelectedItem(mFragmentNameList.indexOf(fragment.getClass()));
        String tag = fragment.getClass().getName();
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 1) {
            FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(1);
            manager.popBackStackImmediate(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }           //clear all except lowest
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .setCustomAnimations(transition1, transition2)
                .replace(R.id.content_frame, fragment, tag);
        if (fragment.getClass() != VaultsListFragment_.class)
            transaction = transaction
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(tag);
        transaction.commit();
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
        Storage.deleteTemp(); //Cleanup every time
        VaultHolder.getInstance().clear();
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

    private void hideMenuItems(Menu menu, boolean visible) {
        for (int i = 0; i < menu.size(); i++)
            menu.getItem(i).setVisible(visible);
    }
}
