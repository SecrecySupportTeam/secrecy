package com.doplgangr.secrecy;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.doplgangr.secrecy.Settings.SettingsFragment_;
import com.doplgangr.secrecy.R;
import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_settings)
public class SettingsActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        Fragment mfragment = getSupportFragmentManager().findFragmentByTag("settingsfragment");
        if ((savedInstanceState==null)&&(mfragment==null))
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment_(), "settingsfragment")
                    .commit();

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }
}
