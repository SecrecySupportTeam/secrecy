package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

import de.greenrobot.event.EventBus;

@EActivity(R.layout.activity_files)
@OptionsMenu(R.menu.main)
public class FilesActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnFragmentFinishListener {
    FragmentManager fragmentManager;
    @Extra(Config.vault_extra)
    String vault;
    @Extra(Config.password_extra)
    String password;

    @AfterViews
    void onCreate() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fadeout);
        fragmentManager = getSupportFragmentManager();
        FilesListFragment_ fragment = new FilesListFragment_();
        Bundle bundle = new Bundle();
        bundle.putString(Config.vault_extra, vault);
        bundle.putString(Config.password_extra, password);
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onPause() {
        overridePendingTransition(R.anim.fadein, R.anim.slide_out_right);
        super.onPause();
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
        switchFragment(fragment);
    }

    void switchFragment(final Fragment fragment) {
        String tag = fragment.getClass().getName();
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void onDestroy() {
        Storage.deleteTemp(); //Cleanup every time
        EventBus.getDefault().post(new shouldRefresh());
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        EventBus.getDefault().post(new OnBackPressedEvent(this));
    }


    public void onEventMainThread(FilesListFragment.OnBackPressedUnhandledEvent event) {
        //Back is pressed. should continue with default activity.
        if (event.activity == this)
            super.onBackPressed();
    }

    @OptionsItem(R.id.home)
    void supporthomePressed() {
        onBackPressed();
    }

    @OptionsItem(android.R.id.home)
    void homePressed() {
        onBackPressed();
    }


    public class OnBackPressedEvent {
        public Activity activity;

        public OnBackPressedEvent(Activity activity) {
            this.activity = activity;
        }
    }

    public class shouldRefresh {

    }

}
