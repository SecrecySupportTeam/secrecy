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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Events.NewFileEvent;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.Jobs.InitializeVaultJob;
import com.doplgangr.secrecy.Listeners;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.nineoldandroids.view.ViewHelper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;

import java.io.File;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;


@EFragment(R.layout.list_file)
@OptionsMenu(R.menu.filelist)
public class FilesListFragment extends FileViewer {
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    @ViewById(android.R.id.list)
    ListView listView = null;
    @ViewById(R.id.gridView)
    GridView gridView = null;
    @ViewById(R.id.nothing)
    View nothing;
    @ViewById(R.id.progressBar)
    ProgressBar addFilepBar;

    @ViewById(R.id.actionBarTitle)
    TextView mActionBarTitle;
    @ViewById(R.id.header)
    View mHeader;

    @ViewById(R.id.tag)
    TextView mTag;
    @OptionsMenuItem(R.id.action_switch_interface)
    MenuItem switchInterface;
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    @FragmentArg(Config.vault_extra)
    String vault;
    @FragmentArg(Config.password_extra)
    String password;
    private Vault secret;
    private FilesListAdapter adapter;
    private int decryptCounter = 0;
    private boolean isGallery = false;
    private boolean attached = false;
    private AbsListView mListView;
    private CustomActionMode mActionMode;

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        switchInterface.setIcon(isGallery ? R.drawable.ic_list : R.drawable.ic_gallery);
    }

    @UiThread
    void switchView(View parentView, int showView) {
        if (parentView == null)
            return;
        FilesListAdapter.ViewHolder holder = (FilesListAdapter.ViewHolder) parentView.getTag();

        ViewAnimator viewAnimator = holder.animator;
        viewAnimator.setInAnimation(context, R.anim.slide_down);
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
        holder.page = viewIndex;
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

    @Background
    @Override
    void onCreate() {
        context = (ActionBarActivity) getActivity();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        CustomApp.jobManager.addJobInBackground(new InitializeVaultJob(vault, password));
    }

    @Background(id = Config.cancellable_task)
    void addFiles() {
        secret.iterateAllFiles(
                new Vault.onFileFoundListener() {
                    @Override
                    public void dothis(EncryptedFile encryptedFile) {
                        addToList(encryptedFile);
                    }
                });
    }

    public void onEventMainThread(NewFileEvent event) {
        // Add new file to the list, sort it to its alphabetical position, and highlight
        // it with smooth scrolling.

        if ((adapter != null) && (attached)) {
            Util.toast(context,
                    CustomApp.context.getString(R.string.Files__add_successful),
                    Toast.LENGTH_SHORT);
            addToList(event.encryptedFile);
            adapter.sort();
            int index = adapter.getItemId(event.encryptedFile);
            if (index != -1)
                listView.smoothScrollToPosition(index);
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
                            try {
                                if (secret.updateFromECBVault(password)) {
                                    Util.alert(
                                            context,
                                            getString(R.string.Error__vault_updated),
                                            getString(R.string.Error__vault_updated_message),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    finish(); //Done for now -_-'
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
                                                    finish(); //Done for now -_-'
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
                                                finish(); //Done for now -_-'
                                            }
                                        },
                                        null
                                );
                            }
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish(); //Done for now -_-'
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
                            finish(); //Done for now -_-'
                        }
                    },
                    null
            );
            return;
        }

        addFiles();
        mActionBarTitle.setText(secret.getName());
        adapter = new FilesListAdapter(context,
                isGallery ? R.layout.gallery_item : R.layout.file_item);
        setupViews();
    }

    public void onEventMainThread(FilesActivity.OnBackPressedEvent onBackPressedEvent) {
        //Back is pressed. End action mode if it is started.

        if (onBackPressedEvent.activity == context && mActionMode != null && mActionMode.isActionMode)
            mActionMode.endActionMode();
        else
            EventBus.getDefault().post(new OnBackPressedUnhandledEvent(onBackPressedEvent.activity));
    }

    @UiThread
    void setupViews() {
        mTag.setText(isGallery ? R.string.Page_header__gallery : R.string.Page_header__files);
        mListView = isGallery ? gridView : listView;
        if (isGallery)
            gridView.setAdapter(adapter);
        else
            listView.setAdapter(adapter);
        listView.setVisibility(View.GONE);
        gridView.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        context.supportInvalidateOptionsMenu();
        context.getSupportActionBar().setTitle("");
        mListView.setEmptyView(nothing);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
                if (mActionMode != null) {
                    mActionMode.select(i, view);
                    return;
                }
                if (isGallery) {

                    Intent intent = new Intent(context, FilePhotoFragment_.class);
                    intent.putExtra(Config.gallery_item_extra, i);
                    onPauseDecision.startActivity();
                    startActivity(intent);
                } else {

                    EncryptedFile encryptedFile = adapter.getItem(i);

                    if (!encryptedFile.getIsDecrypting()) {
                        ProgressBar pBar = (ProgressBar) view.findViewById(R.id.progressBar);
                        switchView(view, R.id.DecryptLayout);
                        Listeners.EmptyListener onFinish = new Listeners.EmptyListener() {
                            @Override
                            public void run() {
                                switchView(view, R.id.dataLayout);
                            }
                        };
                        decrypt(encryptedFile, pBar, onFinish);
                    } else
                        Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
                }
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionMode == null)
                    mActionMode = new CustomActionMode();
                mActionMode.startActionMode();
                // Start the CAB using the ActionMode.Callback defined above
                mActionMode.select(i, view);
                //switchView(view, R.id.file_actions_layout);
                //mListView.setOnClickListener(null);
                return true;
            }
        });

        mListView.setOnScrollListener(new MaterialScroll().listener);
    }

    @UiThread
    void addToList(EncryptedFile encryptedFile) {
        adapter.add(encryptedFile);
    }

    @Background(id = Config.cancellable_task)
    @Override
    void decrypt(EncryptedFile encryptedFile, final ProgressBar pBar, Listeners.EmptyListener onFinish) {
        super.decrypt(encryptedFile, pBar, onFinish);
    }

    @Background(id = Config.cancellable_task)
    void decrypt_and_save(EncryptedFile encryptedFile, final ProgressBar pBar, final Listeners.EmptyListener onFinish) {
        File tempFile = super.getFile(encryptedFile, pBar, onFinish);
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

    @OptionsItem(R.id.action_switch_interface)
    void switchInterface() {
        isGallery = !isGallery;
        onCreate();
    }

    @OptionsItem(R.id.action_change_passphrase)
    void changePassphrase() {
        final View dialogView = View.inflate(context, R.layout.change_passphrase, null);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Vault__change_passphrase))
                .setView(dialogView)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String oldPassphrase = ((EditText) dialogView.findViewById(R.id.oldPassphrase)).getText().toString();
                        String newPassphrase = ((EditText) dialogView.findViewById(R.id.newPassphrase)).getText().toString();
                        String confirmNewPassphrase = ((EditText) dialogView.findViewById(R.id.confirmPassphrase)).getText().toString();

                        if (newPassphrase.length() == 0) {
                            Util.alert(context,
                                    CustomApp.context.getString(R.string.Error__change_passphrase_failed),
                                    CustomApp.context.getString(R.string.Error__passphrase_empty_message),
                                    Util.emptyClickListener,
                                    null
                            );
                            return;
                        }
                        if (!newPassphrase.equals(confirmNewPassphrase)) {
                            Util.alert(context,
                                    CustomApp.context.getString(R.string.Error__change_passphrase_failed),
                                    CustomApp.context.getString(R.string.Error__passphrase_no_match_message),
                                    Util.emptyClickListener,
                                    null
                            );
                            return;
                        }
                        if (!secret.changePassphrase(oldPassphrase, newPassphrase)) {
                            Util.alert(context,
                                    CustomApp.context.getString(R.string.Error__change_passphrase_failed),
                                    CustomApp.context.getString(R.string.Error__change_passphrase_failed_message),
                                    Util.emptyClickListener,
                                    null
                            );
                        } else {
                            Util.alert(context,
                                    CustomApp.context.getString(R.string.Vault__change_passphrase_ok),
                                    CustomApp.context.getString(R.string.Vault__change_passphrase_ok_message),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            context.finish();
                                        }
                                    }
                            );
                        }
                    }
                })
                .setNegativeButton(R.string.CANCEL, Util.emptyClickListener)
                .show();
    }

    @OptionsItem(R.id.action_delete_vault)
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

    @OptionsItem(R.id.action_add_file)
    void myMethod() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.Dialog_header__pick_file));
        try {
            startActivityForResult(intent, REQUEST_CODE);
            onPauseDecision.startActivity();
        } catch (ActivityNotFoundException e) {
            intent = new Intent(context, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
            onPauseDecision.startActivity();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        onPauseDecision.finishActivity();
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data.getData() != null) {
            Util.log("intent received=", data.getData().toString(), data.getData().getLastPathSegment());
            addFile(secret, data.getData());
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.toast(context, getString(R.string.Error__no_file_selected), 4000);
        }
    }

    @OptionsItem(R.id.action_decrypt)
    void decryptCurrentItem() {
        final ArrayList<FilesListAdapter.ViewNIndex> adapterSelected =
                new ArrayList<FilesListAdapter.ViewNIndex>(adapter.getSelected());
        for (final FilesListAdapter.ViewNIndex object : adapterSelected) {
            int position = object.index;
            if (adapter.hasIndex(position)) {
                EncryptedFile encryptedFile = adapter.getItem(position);
                final View mView =
                        ((FilesListAdapter.ViewHolder) object.view.getTag()).selected ?
                                object.view :
                                null;
                if (!encryptedFile.getIsDecrypting()) {
                    decryptCounter++;
                    switchView(mView, R.id.DecryptLayout);
                    ProgressBar pBar =
                            mView != null ?
                                    (ProgressBar) mView.findViewById(R.id.progressBar) :
                                    null;
                    Listeners.EmptyListener onFinish = new Listeners.EmptyListener() {
                        @Override
                        public void run() {
                            decryptCounter--;
                            switchView(mView, R.id.dataLayout);
                            if (decryptCounter == 0 && attached)
                                Util.toast(context, getString(R.string.Files__save_to_SD), Toast.LENGTH_SHORT);
                        }
                    };
                    if (attached)
                        decrypt_and_save(encryptedFile, pBar, onFinish);
                } else if (attached)
                    Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
            }
        }
        mActionMode.endActionMode();
    }

    @OptionsItem(R.id.action_send)
    void sendRaw() {
        ArrayList<DecryptArgHolder> Args = new ArrayList<DecryptArgHolder>();
        for (FilesListAdapter.ViewNIndex object : adapter.getSelected()) {
            int position = object.index;
            if (adapter.hasIndex(position)) {
                EncryptedFile encryptedFile = adapter.getItem(position);
                final View mView = ((FilesListAdapter.ViewHolder) object.view.getTag()).selected ?
                        object.view :
                        null;
                if (!encryptedFile.getIsDecrypting()) {
                    switchView(mView, R.id.DecryptLayout);
                    ProgressBar pBar = mView != null ?
                            (ProgressBar) mView.findViewById(R.id.progressBar) :
                            null;
                    Listeners.EmptyListener onFinish = new Listeners.EmptyListener() {
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
        mActionMode.endActionMode();
    }

    @OptionsItem(R.id.action_delete)
    void deleteCurrentItem() {
        final ArrayList<FilesListAdapter.ViewNIndex> adapterSelected =
                new ArrayList<FilesListAdapter.ViewNIndex>(adapter.getSelected());
        DialogInterface.OnClickListener positive = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (FilesListAdapter.ViewNIndex object : adapterSelected) {
                    int position = object.index;
                    if (adapter.hasIndex(object.index))
                        if (!adapter.getItem(position).getIsDecrypting()) {
                            secret.deleteFile(adapter.getItem(position));
                            adapter.remove(position);
                        } else if (attached)
                            Util.toast(context, getString(R.string.Error__already_decrypting_delete), Toast.LENGTH_SHORT);
                }
            }
        };
        String FilesToDelete = "\n";
        for (FilesListAdapter.ViewNIndex object : adapterSelected)
            if (adapter.hasIndex(object.index))
                FilesToDelete += "- " + adapter.getItem(object.index).getDecryptedFileName() + "\n";
        DialogInterface.OnClickListener negative = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        };
        Util.alert(context,
                getString(R.string.Files__delete),
                String.format(getString(R.string.Files__delete_message), FilesToDelete),
                positive,
                negative
        );
        mActionMode.endActionMode();
    }

    @Override
    void afterDecrypt(Intent newIntent, Intent altIntent) {
        if (attached)
            super.afterDecrypt(newIntent, altIntent);       // check if fragment is attached.
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        if (mActionMode != null && mActionMode.isActionMode)
            inflater.inflate(R.menu.file_action, menu);
        else
            inflater.inflate(R.menu.filelist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    class DecryptArgHolder {
        public EncryptedFile encryptedFile;
        public ProgressBar pBar;
        public Listeners.EmptyListener onFinish;

        public DecryptArgHolder(EncryptedFile encryptedFile, ProgressBar pBar, Listeners.EmptyListener onFinish) {
            this.encryptedFile = encryptedFile;
            this.pBar = pBar;
            this.onFinish = onFinish;
        }
    }

    class CustomActionMode {
        public boolean isActionMode = false;

        void startActionMode() {
            isActionMode = true;
            context.supportInvalidateOptionsMenu();
        }

        void endActionMode() {
            adapter.notifyDataSetChanged();
            for (int i = 0; i < mListView.getChildCount(); i++) {
                View child = mListView.getChildAt(i);
                ((FrameLayout) child.findViewById(R.id.frame))
                        .setForeground(null);
            }
            adapter.clearSelected();
            isActionMode = false;
            new MaterialScroll().listener.onScroll(null, 0, 0, 0); //reset everything
            context.supportInvalidateOptionsMenu();
        }

        void select(int position, View mView) {
            FilesListAdapter.ViewHolder viewHolder = (FilesListAdapter.ViewHolder) mView.getTag();
            viewHolder.selected = adapter.select(position, mView);
            mView.setTag(viewHolder);
            ((FrameLayout) mView.findViewById(R.id.frame))
                    .setForeground(viewHolder.selected ?
                            selector :
                            null);
            new MaterialScroll().setTitle(
                    String.format(getString(R.string.Files__number_selected),
                            adapter.getSelected().size()));
            if (adapter.getSelected().size() == 0)
                endActionMode();
        }

    }

    class MaterialScroll {

        public int getScrollY() {
            View c = mListView.getChildAt(0);
            if (c == null) {
                return 0;
            }

            int firstVisiblePosition = mListView.getFirstVisiblePosition();
            int top = c.getTop();

            int headerHeight = mHeader.getHeight() + mTag.getHeight();
            if (firstVisiblePosition >= 1) {
                headerHeight = mTag.getHeight();
            }

            return -top + firstVisiblePosition * c.getHeight() + headerHeight;
        }

        public void setTitle(CharSequence chars) {
            context.getSupportActionBar().setTitle(chars);
        }

        public AbsListView.OnScrollListener listener = new AbsListView.OnScrollListener() {
            int mHeaderTextHeight = context.getResources().getDimensionPixelSize(R.dimen.header_text_height);
            int mActionBarHeight = context.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                onScroll(absListView, i, 0, 0);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                int scrollY = getScrollY();
                //sticky actionbar
                if (scrollY >= 0) {
                    if (!(mActionMode != null && mActionMode.isActionMode))
                        setTitle(getScrollY() > mActionBarHeight ? secret.getName() : "");
                    ViewHelper.setTranslationY(mHeader, Math.max(-scrollY, -mHeaderTextHeight));
                    mActionBarTitle.setVisibility(scrollY > mActionBarHeight ? View.GONE : View.VISIBLE);
                    ViewHelper.setTranslationY(mTag, -scrollY);
                }
            }
        };


    }

    public class OnBackPressedUnhandledEvent {
        public Activity activity;

        public OnBackPressedUnhandledEvent(Activity activity) {
            this.activity = activity;
        }
    }
}