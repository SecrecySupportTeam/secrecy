package com.doplgangr.secrecy.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.FileSystem.Encryption.VaultHolder;
import com.doplgangr.secrecy.Jobs.AddFileJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;

import java.util.ArrayList;

@EActivity(R.layout.activity_main)
public class FileImportActivity extends ActionBarActivity
        implements
        VaultsListFragment.OnVaultSelectedListener,
        VaultsListFragment.OnFragmentFinishListener {
    Vault secret;

    @AfterViews
    void afterViews() {
        FileImportFragment_ fragment = new FileImportFragment_();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "mainactivitycontent")
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
