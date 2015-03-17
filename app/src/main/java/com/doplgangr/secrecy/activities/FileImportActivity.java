package com.doplgangr.secrecy.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;
import com.doplgangr.secrecy.events.AddingFileDoneEvent;
import com.doplgangr.secrecy.events.AddingFileEvent;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;
import com.doplgangr.secrecy.jobs.AddFileJob;
import com.doplgangr.secrecy.fragments.FileImportFragment;
import com.doplgangr.secrecy.adapters.VaultsListFragment;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class FileImportActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnVaultSelectedListener,
        VaultsListFragment.OnFragmentFinishListener {
    private static final int NotificationID = 1011;
    private Vault secret;
    private Toolbar mToolbar;
    //Notifications
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        FileImportFragment fragment = new FileImportFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.drawer_layout, fragment, "mainactivitycontent")   //Replace the whole drawer layout
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void onVaultSelected(String vault, String passphrase) {
        secret = VaultHolder.getInstance().createAndRetrieveVault(vault, passphrase);
        if (secret.wrongPass) {
            Util.alert(
                    this,
                    getString(R.string.Error__open_vault),
                    getString(R.string.Error__open_vault_message),
                    Util.emptyClickListener,
                    null
            );
            return;
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(CustomApp.context.getString(R.string.Files__adding))
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setOngoing(true);
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(NotificationID, mBuilder.build());
        if (Intent.ACTION_SEND.equals(action) && type != null)
            handleSendInBackground(intent);
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null)
            handleSendMultipleInBackground(intent); // Handle multiple images being sent
    }

    void handleSendInBackground(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null)
                    handleData(uri);
                done();
            }
        }).start();
    }

    void handleSendMultipleInBackground(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Uri uri : uris)
                    if (uri != null)
                        handleData(uri);
                done();
            }
        }).start();
    }

    void handleData(final Uri data) {
        CustomApp.jobManager.addJobInBackground(new AddFileJob(this, secret, data));
    }

    public void onEventMainThread(AddingFileEvent event) {
        if (event.vaultToAdd != secret)
            return;
        if (mBuilder != null) {
            mBuilder.setContentText(event.fileToAdd);
            mNotifyManager.notify(NotificationID, mBuilder.build());
        }
    }

    public void onEventMainThread(AddingFileDoneEvent event) {
        if (event.vault != secret)
            return;
        if (mBuilder != null) {
            mBuilder.setProgress(0, 0, false)
                    .setContentText(CustomApp.context.getString(R.string.Files__adding_finish))
                    .setOngoing(false);
            mNotifyManager.notify(NotificationID, mBuilder.build());
        }
    }

    void done() {
        //Intent intent = new Intent(context, ListFileActivity_.class);
        //intent.putExtra(Config.vault_extra, secret.name);
        //intent.putExtra(Config.password_extra, password);
        //startActivity(intent);
        Util.toast(this, getString(R.string.Import__finish), Toast.LENGTH_SHORT);
        finish();
    }

    @Override
    public void onFinish(Fragment fragment) {
    }

    @Override
    public void onNew(Bundle bundle, Fragment fragment) {
    }
}
