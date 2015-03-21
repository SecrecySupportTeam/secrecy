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

package com.doplgangr.secrecy.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.activities.FilePhotoActivity;
import com.doplgangr.secrecy.activities.FilesActivity;
import com.doplgangr.secrecy.events.AddingFileDoneEvent;
import com.doplgangr.secrecy.events.AddingFileEvent;
import com.doplgangr.secrecy.events.BackUpDoneEvent;
import com.doplgangr.secrecy.events.DecryptingFileDoneEvent;
import com.doplgangr.secrecy.events.NewFileEvent;
import com.doplgangr.secrecy.exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.jobs.BackupJob;
import com.doplgangr.secrecy.jobs.InitializeVaultJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;
import com.doplgangr.secrecy.adapters.FilesListAdapter;
import com.ipaulpro.afilechooser.FileChooserActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;

public class FilesListFragment extends FileViewer {
    private static final int REQUEST_CODE = 6384;
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<>();
    private static final int NotificationID = 1820;
    private RecyclerView recyclerView;
    private ProgressBar addFilepBar;
    private TextView mTag;
    private String vault;
    private String password;
    private Vault secret;
    private FilesListAdapter mAdapter;
    private FilesListAdapter listAdapter;
    private FilesListAdapter galleryAdapter;
    private int decryptCounter = 0;
    private boolean isGallery = false;
    private boolean attached = false;
    //Notifications
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    private ProgressDialog mInitializeDialog;

    private RecyclerView.LayoutManager linearLayout;
    private RecyclerView.LayoutManager gridLayout;

    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_file, container, false);
        addFilepBar = (ProgressBar) view.findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) view.findViewById(R.id.file_list_recycler_view);
        mTag = (TextView) view.findViewById(R.id.tag);

        linearLayout = new LinearLayoutManager(context);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);

        ActionBar ab = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setTitle(vault);
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle extras = getActivity().getIntent().getExtras();

        vault = extras.getString(Config.vault_extra);
        password = extras.getString(Config.password_extra);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        setRetainInstance(true);

        context = (ActionBarActivity) getActivity();
        if (context == null) {
            return;
        }
        context.getSupportActionBar().setTitle(vault);

        gridLayout = new GridLayoutManager(context, 3);
        listAdapter = new FilesListAdapter(context, false);
        galleryAdapter = new FilesListAdapter(context, true);
        mAdapter = listAdapter;

        if (secret == null) {
            mInitializeDialog = new ProgressDialog(context);
            mInitializeDialog.setIndeterminate(true);
            mInitializeDialog.setMessage(context.getString(R.string.Vault__initializing));
            mInitializeDialog.setCancelable(false);
            mInitializeDialog.show();

            CustomApp.jobManager.addJobInBackground(new InitializeVaultJob(vault, password));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filelist, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_switch_interface);
        item.setIcon(isGallery ? R.drawable.ic_list : R.drawable.ic_gallery);
    }

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_action, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_send:
                    sendRawSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_decrypt:
                    decryptSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    deleteSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_rename:
                    renameSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_select_all:
                    selectAll();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mAdapter.clearSelected();
        }
    };

    void switchView(final View frame, final int showView) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (frame == null) {
                    return;
                }
                FilesListAdapter.ViewHolder holder = (FilesListAdapter.ViewHolder) frame.getTag();
                ViewAnimator viewAnimator = holder.animator;
                int viewIndex = 0;
                switch (showView) {
                    case R.id.dataLayout:
                        viewIndex = 0;
                        break;
                    case R.id.DecryptLayout:
                        viewIndex = 1;
                        break;
                }
                viewAnimator.setDisplayedChild(viewIndex);
                viewAnimator.setInAnimation(null);
                holder.page = viewIndex;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        attached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        attached = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(mAdapter);
        EventBus.getDefault().unregister(listAdapter);
        EventBus.getDefault().unregister(galleryAdapter);
        EventBus.getDefault().unregister(this);
    }

    void addFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                secret.iterateAllFiles(
                        new Vault.onFileFoundListener() {
                            @Override
                            public void dothis(EncryptedFile encryptedFile) {
                                addToList(encryptedFile);
                            }
                        });
            }
        }).start();
    }

    void updateVaultInBackground(final ProgressDialog progress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (secret.updateFromECBVault(password)) {
                        Util.alert(
                                context,
                                getString(R.string.Vault__vault_updated),
                                getString(R.string.Vault__vault_updated_message),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                },
                                null
                        );
                    } else {
                        secret.ecbUpdateFailed();
                        Util.alert(
                                context,
                                getString(R.string.Error__updating_vault),
                                getString(R.string.Error__updating_vault_message),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                },
                                null
                        );
                    }
                } catch (Exception e) {
                    secret.ecbUpdateFailed();
                    Util.alert(
                            context,
                            getString(R.string.Error__updating_vault),
                            getString(R.string.Error__updating_vault_message),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            },
                            null
                    );
                }
                progress.dismiss();
            }
        }).start();
    }

    void setupViews() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.supportInvalidateOptionsMenu();

                FilesListAdapter.OnItemClickListener onItemClickListener = new FilesListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(final View view, int position) {
                        if (mActionMode != null) {
                            select(position);
                            return;
                        }
                        if (isGallery) {
                            Intent intent = new Intent(context, FilePhotoActivity.class);
                            intent.putExtra(Config.gallery_item_extra, position);
                            FilesActivity.onPauseDecision.startActivity();
                            startActivity(intent);
                        } else {
                            EncryptedFile encryptedFile = mAdapter.getItem(position);
                            if (!encryptedFile.getIsDecrypting()) {
                                switchView(view, R.id.DecryptLayout);
                                Runnable onFinish = new Runnable() {
                                    @Override
                                    public void run() {
                                        switchView(view, R.id.dataLayout);
                                    }
                                };
                                decrypt(encryptedFile, onFinish);
                            } else
                                Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
                        }
                    }
                };

                FilesListAdapter.OnItemLongClickListener onItemLongClickListener = new FilesListAdapter.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(View view, int position) {
                        if (mActionMode == null)
                            mActionMode = context.startSupportActionMode(mActionModeCallback);
                        // Start the CAB using the ActionMode.Callback defined above
                        select(position);
                        //switchView(view, R.id.file_actions_layout);
                        //mListView.setOnClickListener(null);
                        return true;
                    }
                };

                listAdapter.setOnItemClickListener(onItemClickListener);
                listAdapter.setOnLongClickListener(onItemLongClickListener);

                galleryAdapter.setOnItemClickListener(onItemClickListener);
                galleryAdapter.setOnLongClickListener(onItemLongClickListener);
            }
        });
    }

    void addToList(final EncryptedFile encryptedFile) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listAdapter.add(encryptedFile);
                galleryAdapter.add(encryptedFile);
                listAdapter.sort();
                galleryAdapter.sort();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    void decrypt(final EncryptedFile encryptedFile, final Runnable onFinish) {
        super.decrypt(encryptedFile, onFinish);
    }

    void decrypt_and_save(final int index, final Runnable onFinish) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EncryptedFile encryptedFile = mAdapter.getItem(index);
                File tempFile = getFile(encryptedFile, onFinish);
                File storedFile = new File(Environment.getExternalStorageDirectory(), encryptedFile.getDecryptedFileName());
                if (tempFile == null) {
                    Util.alert(context,
                            CustomApp.context.getString(R.string.Error__decrypting_file),
                            CustomApp.context.getString(R.string.Error__decrypting_file_message),
                            Util.emptyClickListener,
                            null
                    );
                    return;
                }
                tempFile.renameTo(storedFile);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete_vault:
                deleteVault();
                return true;
            case R.id.action_change_passphrase:
                changePassphrase();
                return true;
            case R.id.action_switch_interface:
                switchInterface();
                return true;
            case R.id.action_backup:
                backUp();
                return true;
            case R.id.action_add_file:
                addFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
            }
    }

    void switchInterface() {
        isGallery = !isGallery;
        mTag.setText(isGallery ? R.string.Page_header__gallery : R.string.Page_header__files);
        if (isGallery) {
            mAdapter = galleryAdapter;
            recyclerView.setLayoutManager(gridLayout);
        } else {
            mAdapter = listAdapter;
            mAdapter.sort();
            recyclerView.setLayoutManager(linearLayout);
        }
        recyclerView.setAdapter(mAdapter);
    }

    void changePassphrase() {
        final View dialogView = View.inflate(context, R.layout.change_passphrase, null);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Vault__change_password))
                .setView(dialogView)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String oldPassphrase = ((EditText) dialogView.findViewById(R.id.oldPassphrase)).getText().toString();
                        String newPassphrase = ((EditText) dialogView.findViewById(R.id.newPassphrase)).getText().toString();
                        String confirmNewPassphrase = ((EditText) dialogView.findViewById(R.id.confirmPassphrase)).getText().toString();

                        ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.setMessage(CustomApp.context.getString(R.string.Vault__changing_password));
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        changePassphraseInBackground(oldPassphrase, newPassphrase, confirmNewPassphrase, progressDialog);
                    }
                })
                .setNegativeButton(R.string.CANCEL, Util.emptyClickListener)
                .show();
    }

    void deleteVault() {
        final EditText passwordView = new EditText(context);
        passwordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordView.setHint(R.string.Vault__confirm_password_hint);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Vault__confirm_delete))
                .setView(passwordView)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String input = passwordView.getText().toString();
                        if (password.equals(input)) {
                            secret.delete();
                            context.finish();
                        } else {
                            Util.alert(context,
                                    CustomApp.context.getString(R.string.Error__delete_password_incorrect),
                                    CustomApp.context.getString(R.string.Error__delete_password_incorrect_message),
                                    Util.emptyClickListener,
                                    null
                            );
                        }
                    }
                })
                .setNegativeButton(R.string.CANCEL, Util.emptyClickListener)
                .show();
    }

    void addFile() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.Dialog_header__pick_file));
        try {
            startActivityForResult(intent, REQUEST_CODE);
            FilesActivity.onPauseDecision.startActivity();
        } catch (ActivityNotFoundException e) {
            intent = new Intent(context, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
            FilesActivity.onPauseDecision.startActivity();
        }
    }

    void backUp() {
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(CustomApp.context.getString(R.string.Backup__title))
                .setContentText(CustomApp.context.getString(R.string.Backup__in_progress))
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setOngoing(true);
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(NotificationID, mBuilder.build());
        File backupFile = new File(Storage.getRoot(), secret.getName() + new Date().getTime() + ".zip");
        try {
            backupFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CustomApp.jobManager.addJobInBackground(new BackupJob(context, new File(secret.getPath()), backupFile));
    }

    void changePassphraseInBackground(final String oldPassphrase, final String newPassphrase,
                                      final String confirmNewPassphrase, final ProgressDialog progressDialog) {
        new Thread(new Runnable() {
            public void run() {
                if (newPassphrase.length() == 0) {
                    progressDialog.dismiss();
                    Util.alert(context,
                            CustomApp.context.getString(R.string.Error__change_password_failed),
                            CustomApp.context.getString(R.string.Error__password_empty_message),
                            Util.emptyClickListener,
                            null
                    );
                    return;
                }
                if (!newPassphrase.equals(confirmNewPassphrase)) {
                    progressDialog.dismiss();
                    Util.alert(context,
                            CustomApp.context.getString(R.string.Error__change_password_failed),
                            CustomApp.context.getString(R.string.Error__password_no_match_message),
                            Util.emptyClickListener,
                            null
                    );
                    return;
                }
                if (!secret.changePassphrase(oldPassphrase, newPassphrase)) {
                    progressDialog.dismiss();
                    Util.alert(context,
                            CustomApp.context.getString(R.string.Error__change_password_failed),
                            CustomApp.context.getString(R.string.Error__change_password_failed_message),
                            Util.emptyClickListener,
                            null
                    );
                } else {
                    Util.alert(context,
                            CustomApp.context.getString(R.string.Vault__change_password_ok),
                            CustomApp.context.getString(R.string.Vault__change_password_ok_message),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    context.finish();
                                }
                            }
                    );
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        FilesActivity.onPauseDecision.finishActivity();
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data.getData() != null) {
            Util.log("intent received=", data.getData().toString(), data.getData().getLastPathSegment());

            mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setContentTitle(CustomApp.context.getString(R.string.Files__adding))
                    .setSmallIcon(R.drawable.ic_stat_alert)
                    .setOngoing(true);
            mBuilder.setProgress(0, 0, true);
            mNotifyManager.notify(NotificationID, mBuilder.build());

            addFileInBackground(secret, data.getData());
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.toast(context, getString(R.string.Error__no_file_selected), 4000);
        }
    }


    public void onEventMainThread(NewFileEvent event) {
        // Add new file to the list, sort it to its alphabetical position, and highlight
        // it with smooth scrolling.

        if ((mAdapter != null) && (attached)) {
            Util.toast(context,
                    CustomApp.context.getString(R.string.Files__add_successful),
                    Toast.LENGTH_SHORT);
            addToList(event.encryptedFile);
            mAdapter.sort();
            int index = mAdapter.getItemId(event.encryptedFile);
            if (index != -1)
                recyclerView.smoothScrollToPosition(index);
        }
    }

    public void onEventMainThread(Vault vault) {
        //The vault finishes initializing, is prepared to be populated.
        secret = vault;
        if (secret.isEcbVault()) {
            Util.alert(
                    context,
                    getString(R.string.Error__old_vault_format),
                    getString(R.string.Error__old_vault_format_message),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Util.alert(
                                    context,
                                    getString(R.string.Upgrade__backup_beforehand),
                                    getString(R.string.Upgrade__backup_beforehand_message),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            backUp();
                                            finish();
                                        }
                                    },
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ProgressDialog progress = new ProgressDialog(context);
                                            progress.setMessage(getString(R.string.Vault_updating));
                                            progress.setIndeterminate(true);
                                            progress.setCancelable(false);
                                            progress.show();
                                            updateVaultInBackground(progress);
                                        }
                                    }
                            );
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    }
            );
            return;
        }

        if (secret.wrongPass) {
            Util.alert(
                    context,
                    getString(R.string.Error__open_vault),
                    getString(R.string.Error__open_vault_message),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    },
                    null
            );
            return;
        }

        addFiles();
        context.setTitle(secret.getName());
        mInitializeDialog.dismiss();
        setupViews();
    }

    public void onEventMainThread(BackUpDoneEvent event) {
        if (!event.backupPath.getAbsolutePath().equals(secret.getPath()))
            return;
        if (mBuilder != null) {
            mBuilder.setProgress(0, 0, false)
                    .setContentText(String.format(CustomApp.context.getString(R.string.Backup__finish), event.backupFile))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(    //For long long text
                            String.format(CustomApp.context.getString(R.string.Backup__finish), event.backupFile)))
                    .setOngoing(false);
            mNotifyManager.notify(NotificationID, mBuilder.build());
        }
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

    public void onEventMainThread(DecryptingFileDoneEvent event) {
        mAdapter.notifyItemChanged(event.index);
        decryptCounter--;

        if (decryptCounter == 0 && attached) {
            Util.toast(context, getString(R.string.Files__save_to_SD), Toast.LENGTH_SHORT);
        }
    }

    void decryptSelectedItems() {
        for (final Integer index : mAdapter.getSelected()) {
            decryptCounter++;
            if (mAdapter.hasIndex(index)) {
                if (attached) {
                    mAdapter.getItem(index).setIsDecrypting(true);
                    mAdapter.notifyItemChanged(index);
                    decrypt_and_save(index, new Runnable() {
                        @Override
                        public void run() {
                            EventBus.getDefault().post(new DecryptingFileDoneEvent(index));
                        }
                    });
                }
            }
        }
    }

    void renameSelectedItems() {
        for (final Integer index : mAdapter.getSelected()) {
            decryptCounter++;
            if (mAdapter.hasIndex(index)) {
                if (attached) {
                    final EditText renameView = new EditText(context);
                    renameView.setText(mAdapter.getItem(index).getDecryptedFileName());

                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.File__rename))
                            .setView(renameView)
                            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String newName = renameView.getText().toString();

                                    try {
                                        mAdapter.getItem(index).rename(newName);
                                    } catch (SecrecyCipherStreamException e) {
                                        e.printStackTrace();
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }

                                    mAdapter.notifyItemChanged(index);
                                }
                            })
                            .setNegativeButton(R.string.CANCEL, Util.emptyClickListener)
                            .show();
                }
            }
        }
    }

    void sendRawSelectedItems() {
        ArrayList<DecryptArgHolder> Args = new ArrayList<DecryptArgHolder>();

        for (final Integer index : mAdapter.getSelected()) {
            if (mAdapter.hasIndex(index)) {
                EncryptedFile encryptedFile = mAdapter.getItem(index);
                final View mView = recyclerView.getChildAt(index);
                if (!encryptedFile.getIsDecrypting()) {
                    switchView(mView, R.id.DecryptLayout);
                    ProgressBar pBar = mView != null ?
                            (ProgressBar) mView.findViewById(R.id.progressBar) :
                            null;
                    Runnable onFinish = new Runnable() {
                        @Override
                        public void run() {
                            switchView(mView, R.id.dataLayout);
                        }
                    };
                    Args.add(new DecryptArgHolder(encryptedFile, pBar, onFinish));
                } else if (attached)
                    Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
            }
        }
        if (attached)
            sendMultiple(Args);
    }

    void selectAll() {
        mAdapter.clearSelected();
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            select(i);
        }
    }

    void deleteSelectedItems() {
        // Hold a local copy of selected values, because action mode is left before
        // Util.alter runs and thus adapter.getSelected is cleared.
        final HashSet<Integer> selected = new HashSet<Integer>(mAdapter.getSelected());
        final List<Integer> deleted = new ArrayList<>();

        String FilesToDelete = "\n";
        for (final Integer index : selected)
            if (mAdapter.hasIndex(index))
                FilesToDelete += "- " + mAdapter.getItem(index).getDecryptedFileName() + "\n";

        Util.alert(context,
                getString(R.string.Files__delete),
                String.format(getString(R.string.Files__delete_message), FilesToDelete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (final Integer index : selected) {
                            if (mAdapter.hasIndex(index))
                                if (!mAdapter.getItem(index).getIsDecrypting()) {
                                    secret.deleteFile(mAdapter.getItem(index));
                                    deleted.add(index);
                                } else if (attached)
                                    Util.toast(context, getString(R.string.Error__already_decrypting_delete), Toast.LENGTH_SHORT);
                        }
                        // Don't modify adapter indices before finished.
                        mAdapter.remove(deleted);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }
        );
    }

    @Override
    void afterDecrypt(Intent newIntent, Intent altIntent) {
        if (attached)
            super.afterDecrypt(newIntent, altIntent);       // check if fragment is attached.
    }

    void select(int position) {
        mAdapter.select(position);
        mAdapter.notifyItemChanged(position);

        if (mActionMode == null)
            return;

        mActionMode.setTitle(
                String.format(getString(R.string.Files__number_selected),
                        mAdapter.getSelected().size()));
        MenuItem renameButton = mActionMode.getMenu().findItem(R.id.action_rename);

        if (mAdapter.getSelected().size() == 1)
            renameButton.setVisible(true);
        else
            renameButton.setVisible(false);

        if (mAdapter.getSelected().size() == 0)
            mActionMode.finish();
    }

    class DecryptArgHolder {
        public final EncryptedFile encryptedFile;
        public final ProgressBar pBar;
        public final Runnable onFinish;

        public DecryptArgHolder(EncryptedFile encryptedFile, ProgressBar pBar, Runnable onFinish) {
            this.encryptedFile = encryptedFile;
            this.pBar = pBar;
            this.onFinish = onFinish;
        }
    }

    public class OnBackPressedUnhandledEvent {
        public final Activity activity;

        public OnBackPressedUnhandledEvent(Activity activity) {
            this.activity = activity;
        }
    }
}