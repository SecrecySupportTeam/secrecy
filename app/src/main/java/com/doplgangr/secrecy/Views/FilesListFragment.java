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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.EmptyListener;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;
import org.androidannotations.api.BackgroundExecutor;

import java.io.File;
import java.util.ArrayList;


@EFragment(R.layout.list_file)
@OptionsMenu(R.menu.filelist)
public class FilesListFragment extends FileViewer {
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    @ViewById(android.R.id.list)
    ListView mListView = null;
    @ViewById(R.id.nothing)
    View nothing;
    @ViewById(R.id.progressBar)
    ProgressBar addFilepBar;
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    @FragmentArg(Config.vault_extra)
    String vault;
    @FragmentArg(Config.password_extra)
    String password;

    private Vault secret;
    private FilesListAdapter adapter;
    private ActionMode mActionMode;
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
                // case R.id.action_send_secure:
                //    sendSecure();
                //     mode.finish();
                //     return true;
                case R.id.action_decrypt:
                    decryptCurrentItem();
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    deleteCurrentItem();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        void sendSecure() {
            Intent newIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> Uris = new ArrayList<Uri>();
            for (Integer position : adapter.getSelected())
                Uris.add(Uri.fromFile(adapter.getItem(position).getFile()));
            newIntent.setType("text/plain");
            newIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, Uris);

            startActivity(Intent.createChooser(newIntent, getString(R.string.send_file_dialog)));
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            adapter.notifyDataSetChanged();
            for (int i = 0; i < mListView.getChildCount(); i++) {
                View child = mListView.getChildAt(i);
                ((FrameLayout) child.findViewById(R.id.frame))
                        .setForeground(null);
            }
            adapter.clearSelected();
        }
    };
    private VaultsListFragment.OnFragmentFinishListener mFinishListener;

    @UiThread
    void switchView(View parentView, int showView) {
        parentView.findViewById(R.id.DecryptLayout).setVisibility(View.GONE);
        parentView.findViewById(R.id.dataLayout).setVisibility(View.GONE);
        View shownView = parentView.findViewById(showView);
        shownView.setVisibility(View.VISIBLE);
        Animation slide = AnimationUtils.loadAnimation(context, R.anim.slide_down);
        shownView.setAnimation(slide);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFinishListener = (VaultsListFragment.OnFragmentFinishListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @Background(id = Config.cancellable_task)
    @Override
    void onCreate() {
        context = (ActionBarActivity) getActivity();
        secret = new Vault(vault, password);
        adapter = new FilesListAdapter(context);
        setupViews();
        addFiles();
    }

    @Background(id = Config.cancellable_task)
    void addFiles() {
        secret.iterateAllFiles(
                new Vault.onFileFoundListener() {
                    @Override
                    public void dothis(com.doplgangr.secrecy.FileSystem.File file) {
                        addToList(file);
                    }
                });
    }

    @UiThread
    void setupViews() {
        if (secret.wrongPass) {
            Util.alert(
                    context,
                    getString(R.string.open_failed),
                    getString(R.string.open_failed_message),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish(); //Done for now -_-'
                        }
                    },
                    null
            );
            return;
        }
        mListView.setAdapter(adapter);
        context.getSupportActionBar().setTitle(secret.getName());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View mView, int i, long l) {
                if (mActionMode != null) {
                    select(i, mView);
                    return;
                }
                com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(i);

                if (!file.decrypting) {
                    ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
                    switchView(mView, R.id.DecryptLayout);
                    EmptyListener onFinish = new EmptyListener() {
                        @Override
                        public void run() {
                            switchView(mView, R.id.dataLayout);
                        }
                    };
                    decrypt(file, pBar, onFinish);
                } else
                    Util.toast(context, getString(R.string.error_already_decrypting), Toast.LENGTH_SHORT);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionMode == null)
                    mActionMode = context.startSupportActionMode(mActionModeCallback);
                // Start the CAB using the ActionMode.Callback defined above
                select(i, view);
                //switchView(view, R.id.file_actions_layout);
                //mListView.setOnClickListener(null);
                return true;
            }
        });

        if (secret.getCount() == 0) {
            nothing.setVisibility(View.VISIBLE);
        } else {
            nothing.setVisibility(View.GONE);
        }
        addFilepBar.setVisibility(View.GONE);
    }

    @UiThread
    void addToList(com.doplgangr.secrecy.FileSystem.File file) {
        adapter.add(file);
    }

    @Background
    @Override
    void decrypt(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, EmptyListener onFinish) {
        super.decrypt(file, pBar, onFinish);
    }

    @Background
    void decrypt_and_save(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, final EmptyListener onFinish) {
        File tempFile = super.getFile(file, pBar, onFinish);
        File storedFile = new File(Environment.getExternalStorageDirectory(), file.getName() + "." + file.getType());
        tempFile.renameTo(storedFile);
    }

    @OptionsItem(R.id.action_switch_interface)
    void switchInterface() {
        FilesGalleryFragment_ fragment = new FilesGalleryFragment_();
        Bundle data = new Bundle();
        data.putString(Config.vault_extra, vault);
        data.putString(Config.password_extra, password);
        mFinishListener.onNew(data, fragment);
        mFinishListener.onFinish(this);
    }

    @OptionsItem(R.id.action_add_file)
    void myMethod() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.chooser_title));
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(context, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            Log.d("intent received", data.getData().toString() + " " + data.getData().getLastPathSegment());
            addFilepBar.setVisibility(View.VISIBLE);
            addFile(secret, data);
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.toast(context, getString(R.string.error_no_file_selected), 4000);
        }
    }

    void decryptCurrentItem() {
        final ArrayList<Integer> adapterSelected = new ArrayList<Integer>(adapter.getSelected());
        for (Integer position : adapterSelected) {
            com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(position);
            final View mView = mListView.getChildAt(position);
            if (!file.decrypting) {
                ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
                switchView(mView, R.id.DecryptLayout);
                EmptyListener onFinish = new EmptyListener() {
                    @Override
                    public void run() {
                        switchView(mView, R.id.dataLayout);
                        Util.toast(context, getString(R.string.save_to_SD), Toast.LENGTH_SHORT);
                    }
                };
                decrypt_and_save(file, pBar, onFinish);
            } else
                Util.toast(context, getString(R.string.error_already_decrypting), Toast.LENGTH_SHORT);
        }
    }

    void deleteCurrentItem() {
        final ArrayList<Integer> adapterSelected = new ArrayList<Integer>(adapter.getSelected());
        DialogInterface.OnClickListener positive = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (Integer position : adapterSelected)
                    if (!adapter.getItem(position).decrypting)
                        adapter.getItem(position).delete();
                    else
                        Util.toast(context, getString(R.string.error_delete_decrypting), Toast.LENGTH_SHORT);
                secret.initialize();
                adapter.notifyDataSetChanged();
            }
        };
        String FilesToDelete = "\n";
        for (Integer position : adapter.getSelected())
            FilesToDelete += "- " + adapter.getItem(position).getName() + "\n";
        DialogInterface.OnClickListener negative = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        };
        Util.alert(context,
                getString(R.string.delete_files),
                String.format(getString(R.string.delete_files_message), FilesToDelete),
                positive,
                negative
        );
    }

    void select(int position, View mView) {
        if (adapter.select(position))
            ((FrameLayout) mView.findViewById(R.id.frame))
                    .setForeground(selector);
        else
            ((FrameLayout) mView.findViewById(R.id.frame))
                    .setForeground(null);
        mActionMode.setTitle(String.format(getString(R.string.action_mode_title_no_selection), adapter.getSelected().size()));
    }

    @Override
    void paused() {
        //Do not end activity
    }

    void finish() {
        getActivity().finish();
        BackgroundExecutor.cancelAll(Config.cancellable_task, true);
        //mFinishListener.onFinish(this);
    }
}