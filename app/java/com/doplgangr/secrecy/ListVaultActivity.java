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

package com.doplgangr.secrecy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.flurry.android.FlurryAgent;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@EActivity(R.layout.activity_list_vault)
@OptionsMenu(R.menu.list_vault)
public class ListVaultActivity extends ActionBarActivity {
    @ViewById(android.R.id.list)
    GridView gridView = null;
    @ViewById(R.id.nothing)
    View nothing;
    VaultAdapter adapter;
    ActionMode mActionMode;
    private android.support.v7.view.ActionMode.Callback mActionModeCallback = new android.support.v7.view.ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.vault_action, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(android.support.v7.view.ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteCurrentItem();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(android.support.v7.view.ActionMode mode) {
            mActionMode = null;
            adapter.notifyDataSetChanged();
            for (int i = 0; i < gridView.getChildCount(); i++) {
                View child = gridView.getChildAt(i);
                ((FrameLayout) child.findViewById(R.id.frame))
                        .setForeground(null);
            }
            adapter.clearSelected();
        }
    };
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    Context context = this;

    @AfterViews
    void oncreate() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle(storage.getRoot().getAbsolutePath());
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
            public void onItemClick(AdapterView<?> adapterView, View mView, int i, long l) {
                if (mActionMode != null) {
                    select(i, mView);
                    return;
                }
                open(adapter.getItem(i));

            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionMode == null)
                    mActionMode = startSupportActionMode(mActionModeCallback);
                // Start the CAB using the ActionMode.Callback defined above
                select(i, view);
                return true;
            }
        });

    }
    @OptionsItem(R.id.action_settings)
    void settings() {
        startActivity(new Intent(this,SettingsActivity_.class));
    }

    @OptionsItem(R.id.action_donate)
    void donate(){
        startActivity(new Intent(this,DonationsActivity.class));
    }


    @OptionsItem(R.id.action_add)
    void add() {
        final View dialogView = View.inflate(this, R.layout.new_credentials, null);
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_vault))
                .setMessage(getString(R.string.prompt_credentials))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = ((EditText) dialogView.findViewById(R.id.newName)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.password)).getText().toString();
                        String Confirmpassword = ((EditText) dialogView.findViewById(R.id.confirmPassword)).getText().toString();
                        File directory = new File(storage.getRoot().getAbsolutePath() + "/" + name);
                        if (!password.equals(Confirmpassword) || "".equals(password))
                            passwordWrong();
                        else if (directory.mkdirs()) {
                            try {
                                File file = new File(context.getFilesDir(), ".nomedia");
                                file.delete();
                                file.createNewFile();
                                FileOutputStream outputStream = new FileOutputStream(file);
                                outputStream.write(name.getBytes());
                                outputStream.close();
                                Uri nomediaURI = Uri.fromFile(file);
                                Vault newVault = new Vault(name, password, true);
                                newVault.addFile(context, nomediaURI);
                                file.delete();
                                oncreate();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            failedtocreate();

                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    public void passwordWrong() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_wrong_password_confirmation))
                .setMessage(getString(R.string.error_wrong_password_confirmation_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    void failedtocreate() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_cannot_create_vault))
                .setMessage(getString(R.string.error_cannot_create_vault_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    void select(int position, View mView) {
        if (adapter.select(position))
            ((FrameLayout) mView.findViewById(R.id.frame))
                    .setForeground(selector);
        else
            ((FrameLayout) mView.findViewById(R.id.frame))
                    .setForeground(null);
        mActionMode.setTitle(String.format(getString(R.string.action_mode_no_vaults_selected), adapter.getSelected().size()));
    }

    void open(final String vault) {
        FlurryAgent.logEvent("Vault_open");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle(String.format(getString(R.string.open_vault), vault))
                .setMessage(getString(R.string.type_password))
                .setView(input)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        Intent intent = new Intent(context, ListFileActivity_.class);
                        intent.putExtra(Config.vault_extra, vault);
                        intent.putExtra(Config.password_extra, value);
                        startActivity(intent);
                    }
                }).setNegativeButton(getString(R.string.cancel), Util.emptyClickListener)
                .show();
    }

    void deleteCurrentItem() {
        final ArrayList<Integer> adapterSelected = new ArrayList<Integer>(adapter.getSelected());
        DialogInterface.OnClickListener positive = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (final Integer position : adapterSelected) {
                    FlurryAgent.logEvent("Vault_delete");
                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    new AlertDialog.Builder(context)
                            .setTitle(String.format(getString(R.string.delete_vault), adapter.getItem(position)))
                            .setMessage(getString(R.string.type_password))
                            .setView(input)
                            .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String value = input.getText().toString();
                                    Boolean pwState = new Vault(adapter.getItem(position), value).delete();
                                    if (!pwState)
                                        Util.alert(context,
                                                getString(R.string.error_delete_password_incorrect),
                                                getString(R.string.error_delete_password_incorrect_message),
                                                Util.emptyClickListener,
                                                null
                                        );
                                    adapter.notifyDataSetChanged();
                                    oncreate();
                                }
                            }).setNegativeButton(getString(R.string.cancel), Util.emptyClickListener)
                            .show();
                }
            }
        };
        String vaultsToDelete = "\n";
        for (Integer position : adapter.getSelected())
            vaultsToDelete += "- " + adapter.getItem(position) + "\n";
        DialogInterface.OnClickListener negative = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        };
        Util.alert(this,
                getString(R.string.delete_vault),
                String.format(getString(R.string.delete_vault_confirmation), vaultsToDelete),
                positive,
                negative
        );
    }

    @Override
    protected void onResume() {
        oncreate();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, "HYKWRVV3NS99JXT968K7");
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }
}
