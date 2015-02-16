package com.doplgangr.secrecy.Views;

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
import org.androidannotations.api.BackgroundExecutor;

import de.greenrobot.event.EventBus;

@EActivity(R.layout.activity_files)
public class FilesActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnFragmentFinishListener {
    @Extra(Config.vault_extra)
    String vault;
    @Extra(Config.password_extra)
    String password;
    private FragmentManager fragmentManager;
    private Boolean isConfigChange;

    @AfterViews
    void onCreate() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.fadeout);
        fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(FilesListFragment.class.getName()) == null) {
            FilesListFragment_ fragment = new FilesListFragment_();
            Bundle bundle = new Bundle();
            bundle.putString(Config.vault_extra, vault);
            bundle.putString(Config.password_extra, password);
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction()
                    .replace(R.id.content, fragment, FilesListFragment.class.getName())
                    .commit();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isConfigChange = true;
    }

    // http://steveliles.github.io/porting_ischangingconfigurations_to_api_levels_below_11.html

    @Override
    public boolean isChangingConfigurations() {
        if (android.os.Build.VERSION.SDK_INT >= 11)
            return super.isChangingConfigurations();
        else
            return isConfigChange;
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
    public void onPause() {
        overridePendingTransition(R.anim.fadein, R.anim.slide_out_right);
        super.onPause();
        if (isChangingConfigurations())
            onPauseDecision.startActivity();
        if (onPauseDecision.shouldFinish())
            finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        onPauseDecision.finishActivity();
    }

    @Override
    public void finish() {
        BackgroundExecutor.cancelAll(Config.cancellable_task, false);
        super.finish();
    }

    @Override
    public void onDestroy() {
        Storage.deleteTemp(); //Cleanup every time
        EventBus.getDefault().post(new shouldRefresh());
        super.onDestroy();
    }

    public static class onPauseDecision {
        static Boolean pause = true;

        // An activity is started, should not pause and kill this fragment.
        public static void startActivity() {
            pause = false;
        }

        // Fragment returns to top, allow it to be paused and killed.
        public static void finishActivity() {
            pause = true;
        }

        public static Boolean shouldFinish() {
            return pause;
        }
    }

    public class shouldRefresh {

    }

}
