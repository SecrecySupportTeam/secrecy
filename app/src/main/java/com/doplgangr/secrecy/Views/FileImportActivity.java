package com.doplgangr.secrecy.Views;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Events.AddingFileDoneEvent;
import com.doplgangr.secrecy.Events.AddingFileEvent;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.FileSystem.Encryption.VaultHolder;
import com.doplgangr.secrecy.Jobs.AddFileJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

@EActivity(R.layout.activity_main)
public class FileImportActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnVaultSelectedListener,
        VaultsListFragment.OnFragmentFinishListener {
    private static final int NotificationID = 1011;
    Vault secret;
    @ViewById(R.id.toolbar)
    Toolbar mToolbar;
    //Notifications
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @AfterViews
    void afterViews() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        setSupportActionBar(mToolbar);
        FileImportFragment_ fragment = new FileImportFragment_();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.drawer_layout, fragment, "mainactivitycontent")   //Replace the whole drawer layout
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void onFinish(Fragment fragment) {
    }

    @Override
    public void onNew(Bundle bundle, Fragment fragment) {

    }

    @Override
    public void onVaultSelected(String vault, String password) {
        secret = VaultHolder.getInstance().createAndRetrieveVault(vault, password);
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
            handleSend(intent);
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null)
            handleSendMultiple(intent); // Handle multiple images being sent
    }

    @Background
    void handleSend(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null)
            handleData(uri);
        done();
    }

    @Background
    void handleSendMultiple(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        for (Uri uri : uris)
            if (uri != null)
                handleData(uri);
        done();
    }

    @Background
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

    @UiThread
    public void done() {
        //Intent intent = new Intent(context, ListFileActivity_.class);
        //intent.putExtra(Config.vault_extra, secret.name);
        //intent.putExtra(Config.password_extra, password);
        //startActivity(intent);
        Util.toast(this, getString(R.string.Import__finish), Toast.LENGTH_SHORT);
        finish();
    }

}
