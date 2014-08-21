package com.doplgangr.secrecy.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.widget.Toast;

import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;

import java.io.File;
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
    public void onVaultSelected(String vault, String password) {
        secret = new Vault(vault, password);
        if (secret.wrongPass) {
            Util.alert(
                    this,
                    getString(R.string.open_failed),
                    getString(R.string.open_failed_message),
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
        String filename = secret.addFile(this, data);
        Uri thumbnail = storage.saveThumbnail(this, data, filename);
        if (thumbnail != null) {
            secret.addFile(this, thumbnail);
            storage.purgeFile(new java.io.File(thumbnail.getPath()));
        }
        storage.purgeFile(new File(data.getPath())); //Try to delete original file.
        try {
            this.getContentResolver().delete(data, null, null); //Try to delete under content resolver
        } catch (Exception ignored) {
            //Ignore fail to delete original file
        }
    }

    @UiThread
    public void done() {
        //Intent intent = new Intent(context, ListFileActivity_.class);
        //intent.putExtra(Config.vault_extra, secret.name);
        //intent.putExtra(Config.password_extra, password);
        //startActivity(intent);
        Util.toast(this, getString(R.string.import_finish), Toast.LENGTH_SHORT);
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear(); //no options item should appear here.
        return true;
    }

}
