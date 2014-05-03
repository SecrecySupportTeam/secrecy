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
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.util.ArrayList;

@EActivity(R.layout.activity_list_vault)
public class FileImporter extends ActionBarActivity {
    @ViewById(android.R.id.list)
    GridView gridView = null;
    @ViewById(R.id.nothing)
    View nothing;
    @ViewById(R.id.progressBar)
    ProgressBar addFilepBar;
    VaultAdapter adapter;
    Context context = this;
    Vault secret;

    @AfterViews
    void onCreate() {
        setTitle("Select Vault to place files in");
        ArrayList<String> vaultList = new ArrayList<String>();
        java.io.File root = storage.getRoot();
        java.io.File[] files = root.listFiles();
        for (java.io.File inFile : files)
            if (inFile.isDirectory() && !inFile.equals(storage.getTempFolder()))
                vaultList.add(inFile.getName());
        adapter = new VaultAdapter(this, R.layout.vault_item, vaultList);
        gridView.setAdapter(adapter);
        if (gridView.getCount() == 0)
            nothing.setVisibility(View.VISIBLE);
        else
            nothing.setVisibility(View.GONE);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View mView, final int i, long l) {
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                new AlertDialog.Builder(context)
                        .setTitle(String.format(getString(R.string.open_vault), adapter.getItem(i)))
                        .setMessage(getString(R.string.type_password))
                        .setView(input)
                        .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                addFilepBar.setVisibility(View.VISIBLE);
                                String password = input.getText().toString();
                                secret = new Vault(adapter.getItem(i), password);
                                if (secret.wrongPass) {
                                    Util.alert(
                                            context,
                                            getString(R.string.open_failed),
                                            getString(R.string.open_failed_message),
                                            Util.emptyClickListener,
                                            null
                                    );
                                    addFilepBar.setVisibility(View.GONE);
                                    return;
                                }
                                Intent intent = getIntent();
                                String action = intent.getAction();
                                String type = intent.getType();
                                if (Intent.ACTION_SEND.equals(action) && type != null)
                                    handleSend(intent);
                                else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null)
                                    handleSendMultiple(intent); // Handle multiple images being sent
                                addFilepBar.setVisibility(View.GONE);
                            }
                        }).setNegativeButton(getString(R.string.cancel), Util.emptyClickListener)
                        .show();

            }
        });
    }

    @Background
    void handleSend(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null)
            handleData(uri);
        finish();
    }

    @Background
    void handleSendMultiple(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        for (Uri uri : uris)
            if (uri != null)
                handleData(uri);
        finish();
    }

    @Background
    void handleData(final Uri data) {
        String filename = secret.addFile(this, data);
        Uri thumbnail = storage.saveThumbnail(this, data, filename);
        if (thumbnail != null) {
            secret.addFile(this, thumbnail);
            new java.io.File(thumbnail.getPath()).delete();
        }
        try {
            getContentResolver().delete(data, null, null); //Try to delete under content resolver
        } catch (Exception E) {
        } finally {
            new File(data.getPath()).delete(); //Try to delete original file.
        }
    }

    @Override
    @UiThread
    public void finish() {
        //Intent intent = new Intent(context, ListFileActivity_.class);
        //intent.putExtra(Config.vault_extra, secret.name);
        //intent.putExtra(Config.password_extra, password);
        //startActivity(intent);
        Util.toast(this, getString(R.string.import_finish), Toast.LENGTH_SHORT);
        super.finish();
    }

}
