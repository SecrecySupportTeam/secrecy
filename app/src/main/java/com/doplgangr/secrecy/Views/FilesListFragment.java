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
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.Jobs.AddFileJob;
import com.doplgangr.secrecy.Listeners;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;

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
                case R.id.action_send:
                    sendRaw();
                    mode.finish();
                    return true;
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        switchInterface.setIcon(isGallery ? R.drawable.ic_list : R.drawable.ic_gallery);
    }

    @UiThread
    void switchView(View parentView, int showView) {
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
        try {
            mFinishListener = (VaultsListFragment.OnFragmentFinishListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        attached = false;
    }

    @Background
    @Override
    void onCreate() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        context = (ActionBarActivity) getActivity();
        secret = new Vault(vault, password);
        adapter = new FilesListAdapter(context,
                isGallery ? R.layout.gallery_item : R.layout.file_item);
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
    }

    @Background(id = Config.cancellable_task)
    void addFiles() {
        setupViews();
        secret.iterateAllFiles(
                new Vault.onFileFoundListener() {
                    @Override
                    public void dothis(com.doplgangr.secrecy.FileSystem.File file) {
                        addToList(file);
                    }
                });
    }

    public void onEventMainThread(AddFileJob.NewFileEvent event) {
        // Add new file to the list, sort it to its alphabetical position, and highlight
        // it with smooth scrolling.

        if ((adapter != null) && (attached)) {
            Util.toast(context,
                    CustomApp.context.getString(R.string.Files__add_successful),
                    Toast.LENGTH_SHORT);
            addToList(event.file);
            adapter.sort();
            int index = adapter.getItemId(event.file);
            if (index != -1)
                listView.smoothScrollToPosition(index);
        }
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
        mActionBarTitle.setText(secret.getName());
        mListView.setEmptyView(nothing);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
                if (mActionMode != null) {
                    select(i, view);
                    return;
                }
                if (isGallery) {

                    Intent intent = new Intent(context, FilePhotoFragment_.class);
                    intent.putExtra(Config.vault_extra, vault);
                    intent.putExtra(Config.password_extra, password);
                    intent.putExtra(Config.gallery_item_extra, i);
                    onPauseDecision.startActivity();
                    startActivity(intent);
                } else {

                    com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(i);

                    if (!file.decrypting) {
                        ProgressBar pBar = (ProgressBar) view.findViewById(R.id.progressBar);
                        switchView(view, R.id.DecryptLayout);
                        Listeners.EmptyListener onFinish = new Listeners.EmptyListener() {
                            @Override
                            public void run() {
                                switchView(view, R.id.dataLayout);
                            }
                        };
                        decrypt(file, pBar, onFinish);
                    } else
                        Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
                }
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
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
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
                if (scrollY > 0) {
                    mHeader.setTranslationY(Math.max(-scrollY, -mHeaderTextHeight));

                    ViewGroup.LayoutParams params = mActionBarTitle.getLayoutParams();
                    params.height = scrollY > mActionBarHeight ? mActionBarHeight : mHeaderTextHeight;
                    mActionBarTitle.setLayoutParams(params);
                    mTag.setTranslationY(-scrollY);
                }
            }

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
        });
    }

    @UiThread
    void addToList(com.doplgangr.secrecy.FileSystem.File file) {
        adapter.add(file);
    }

    @Background(id = Config.cancellable_task)
    @Override
    void decrypt(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, Listeners.EmptyListener onFinish) {
        super.decrypt(file, pBar, onFinish);
    }

    @Background(id = Config.cancellable_task)
    void decrypt_and_save(com.doplgangr.secrecy.FileSystem.File file, final ProgressBar pBar, final Listeners.EmptyListener onFinish) {
        File tempFile = super.getFile(file, pBar, onFinish);
        File storedFile = new File(Environment.getExternalStorageDirectory(), file.getName() + "." + file.getType());
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

    void decryptCurrentItem() {
        final ArrayList<FilesListAdapter.ViewNIndex> adapterSelected =
                new ArrayList<FilesListAdapter.ViewNIndex>(adapter.getSelected());
        for (final FilesListAdapter.ViewNIndex object : adapterSelected) {
            int position = object.index;
            if (adapter.hasIndex(position)) {
                com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(position);
                final View mView = object.view;
                if (!file.decrypting) {
                    decryptCounter++;
                    switchView(mView, R.id.DecryptLayout);
                    ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
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
                        decrypt_and_save(file, pBar, onFinish);
                } else if (attached)
                    Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
            }
        }
    }


    void sendRaw() {
        ArrayList<DecryptArgHolder> Args = new ArrayList<DecryptArgHolder>();
        for (FilesListAdapter.ViewNIndex object : adapter.getSelected()) {
            int position = object.index;
            if (adapter.hasIndex(position)) {
                com.doplgangr.secrecy.FileSystem.File file = adapter.getItem(position);
                final View mView = object.view;
                if (!file.decrypting) {
                    switchView(mView, R.id.DecryptLayout);
                    ProgressBar pBar = (ProgressBar) mView.findViewById(R.id.progressBar);
                    Listeners.EmptyListener onFinish = new Listeners.EmptyListener() {
                        @Override
                        public void run() {
                            switchView(mView, R.id.dataLayout);
                        }
                    };
                    Args.add(new DecryptArgHolder(file, pBar, onFinish));
                } else if (attached)
                    Util.toast(context, getString(R.string.Error__already_decrypting), Toast.LENGTH_SHORT);
            }
        }
        if (attached)
            sendMultiple(Args);
    }


    void deleteCurrentItem() {
        final ArrayList<FilesListAdapter.ViewNIndex> adapterSelected =
                new ArrayList<FilesListAdapter.ViewNIndex>(adapter.getSelected());
        DialogInterface.OnClickListener positive = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (FilesListAdapter.ViewNIndex object : adapterSelected) {
                    int position = object.index;
                    if (adapter.hasIndex(object.index))
                        if (!adapter.getItem(position).decrypting) {
                            adapter.getItem(position).delete();
                            adapter.remove(position);
                        } else if (attached)
                            Util.toast(context, getString(R.string.Error__already_decrypting_delete), Toast.LENGTH_SHORT);
                }
            }
        };
        String FilesToDelete = "\n";
        for (FilesListAdapter.ViewNIndex object : adapterSelected)
            if (adapter.hasIndex(object.index))
                FilesToDelete += "- " + adapter.getItem(object.index).getName() + "\n";
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
    }

    void select(int position, View mView) {
        FilesListAdapter.ViewHolder viewHolder = (FilesListAdapter.ViewHolder) mView.getTag();
        viewHolder.selected = adapter.select(position, mView);
        ((FrameLayout) mView.findViewById(R.id.frame))
                .setForeground(viewHolder.selected ?
                        selector :
                        null);
        mActionMode.setTitle(String.format(getString(R.string.Files__number_selected), adapter.getSelected().size()));
    }

    @Override
    void afterDecrypt(Intent newIntent, Intent altIntent) {
        if (attached)
            super.afterDecrypt(newIntent, altIntent);       // check if fragment is attached.
    }

    class DecryptArgHolder {
        public com.doplgangr.secrecy.FileSystem.File file;
        public ProgressBar pBar;
        public Listeners.EmptyListener onFinish;

        public DecryptArgHolder(com.doplgangr.secrecy.FileSystem.File file, ProgressBar pBar, Listeners.EmptyListener onFinish) {
            this.file = file;
            this.pBar = pBar;
            this.onFinish = onFinish;
        }
    }
}