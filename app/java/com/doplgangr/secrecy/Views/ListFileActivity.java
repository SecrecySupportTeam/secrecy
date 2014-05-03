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

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.EmptyListener;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Premium.PremiumActivity_;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.SettingsActivity_;
import com.doplgangr.secrecy.Util;
import com.flurry.android.FlurryAgent;
import com.ipaulpro.afilechooser.FileChooserActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;


@EActivity(R.layout.list_file)
@OptionsMenu(R.menu.filelist)
public class ListFileActivity extends FileViewer {
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    Vault secret;
    FileAdapter adapter;
    @ViewById(android.R.id.list)
    GridView gridView = null;
    @ViewById(R.id.nothing)
    View nothing;
    @ViewById(R.id.progressBar)
    ProgressBar addFilepBar;
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

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
            for (int i = 0; i < gridView.getChildCount(); i++) {
                View child = gridView.getChildAt(i);
                ((FrameLayout) child.findViewById(R.id.frame))
                        .setForeground(null);
            }
            adapter.clearSelected();
        }
    };
    @Extra(Config.vault_extra)
    String vault;
    @Extra(Config.password_extra)
    String password;

    @AfterViews
    @UiThread
    @Override
    void onCreate() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.fadeout);
        secret = new Vault(vault, password);
        String size = FileUtils.byteCountToDisplaySize(
                FileUtils.sizeOfDirectory(
                        new File(storage.getRoot().getAbsolutePath() + "/" + secret.getName()))
        );
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle("Size on SDcard: " + size);
        if (secret.wrongPass) {
            Util.alert(
                    this,
                    getString(R.string.open_failed),
                    getString(R.string.open_failed_message),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Util.log("onClick called");
                            finish();
                        }
                    },
                    null
            );
            return;
        }
        adapter = new FileAdapter(
                this, R.layout.file_item, secret.files);
        gridView.setAdapter(adapter);
        if (gridView.getCount() == 0) {
            nothing.setVisibility(View.VISIBLE);
        } else {
            nothing.setVisibility(View.GONE);
        }
        addFilepBar.setVisibility(View.GONE);
        getSupportActionBar().setTitle(secret.getName());
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View mView, int i, long l) {
                if (mActionMode != null) {
                    select(i, mView);
                    return;
                }
                com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(i);

                if (!file.decrypting) {
                    ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
                    View view = mView.findViewById(R.id.DecryptLayout);
                    view.setVisibility(View.VISIBLE);
                    view = mView.findViewById(R.id.dataLayout);
                    view.setVisibility(View.GONE);
                    EmptyListener onFinish = new EmptyListener() {
                        @Override
                        public void run() {
                            restoreCryptViews(mView);
                        }
                    };
                    decrypt(file, pBar, onFinish);
                } else
                    Util.toast(context, getString(R.string.error_already_decrypting), Toast.LENGTH_SHORT);
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

    @Background
    @Override
    void decrypt(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, EmptyListener onFinish) {
        super.decrypt(file, pBar, onFinish);
        onCreate();
    }

    @Background
    void decrypt_and_save(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, final EmptyListener onFinish) {
        File tempFile = super.getFile(file, pBar, onFinish);
        File storedFile = new File(Environment.getExternalStorageDirectory(), file.getName() + "." + file.getType());
        tempFile.renameTo(storedFile);
    }

    @OptionsItem(R.id.action_settings)
    void settings() {
        startActivity(new Intent(this, SettingsActivity_.class));
    }

    @OptionsItem(R.id.action_donate)
    void donate() {
        startActivity(new Intent(this, PremiumActivity_.class));
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
            intent = new Intent(this, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            Log.d("intent received", data.getData().toString() + " " + data.getData().getLastPathSegment());
            addFilepBar.setVisibility(View.VISIBLE);
            addFile(data);
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.toast(this, getString(R.string.error_no_file_selected), 4000);
        }
    }

    @Background
    void addFile(final Intent data) {
        String filename = secret.addFile(this, data.getData());
        Uri thumbnail = storage.saveThumbnail(this, data.getData(), filename);
        if (thumbnail != null) {
            secret.addFile(this, thumbnail);
            new File(thumbnail.getPath()).delete();
        }
        Util.alert(context,
                getString(R.string.add_successful),
                getString(R.string.add_successful_message),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            getContentResolver().delete(data.getData(), null, null); //Try to delete under content resolver
                        } catch (Exception E) {
                        } finally {
                            new File(data.getData().getPath()).delete(); //Try to delete original file.
                        }
                    }
                },
                Util.emptyClickListener
        );
        onCreate();
    }

    void decryptCurrentItem() {
        final ArrayList<Integer> adapterSelected = new ArrayList<Integer>(adapter.getSelected());
        for (Integer position : adapterSelected) {
            com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(position);
            final View mView = gridView.getChildAt(position);
            if (!file.decrypting) {
                ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
                View view = mView.findViewById(R.id.DecryptLayout);
                view.setVisibility(View.VISIBLE);
                view = mView.findViewById(R.id.dataLayout);
                view.setVisibility(View.GONE);
                EmptyListener onFinish = new EmptyListener() {
                    @Override
                    public void run() {
                        restoreCryptViews(mView);
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
        Util.alert(this,
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

    @Override
    protected void onResume() {
        onCreate();
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

    @UiThread
    void restoreCryptViews(View mView) {
        View view = mView.findViewById(R.id.DecryptLayout);
        view.setVisibility(View.GONE);
        view = mView.findViewById(R.id.dataLayout);
        view.setVisibility(View.VISIBLE);
    }
}