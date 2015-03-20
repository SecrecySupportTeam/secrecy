package com.doplgangr.secrecy.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.fragments.FilesListFragment;
import com.doplgangr.secrecy.adapters.VaultsListFragment;

import de.greenrobot.event.EventBus;

//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentTransaction;

public class FilesActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnFragmentFinishListener {
    private FragmentManager fragmentManager;
    private Boolean isConfigChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        // Don't keep a screenshot of this window for recent apps
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        Bundle extras = getIntent().getExtras();
        String vault = extras.getString(Config.vault_extra);
        String password = extras.getString(Config.password_extra);

        fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentByTag(FilesListFragment.class.getName()) == null) {
            FilesListFragment fragment = new FilesListFragment();
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
    public void onDestroy() {
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
