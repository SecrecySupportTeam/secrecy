/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with context work for additional information
 * regarding copyright ownership.  The ASF licenses context file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use context file except in compliance
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
import android.app.NotificationManager;
import android.content.Context;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Events.RestoreDoneEvent;
import com.doplgangr.secrecy.Events.RestoringFileEvent;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.FileSystem.Encryption.VaultHolder;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Jobs.RestoreJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.Settings.SettingsFragment_;
import com.doplgangr.secrecy.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;

@EFragment(R.layout.activity_list_vault)
@OptionsMenu(R.menu.list_vault)
public class VaultsListFragment extends Fragment {
    //Vault restore module
    private static final int REQUESTCODE = 1203; //Arbitrary
    @ViewById(R.id.list)
    LinearLayout mLinearView;
    @ViewById(R.id.scrollView)
    ScrollView mScrollView;
    @ViewById(R.id.nothing)
    View nothing;
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    @Pref
    Prefs_ Pref;
    ActionBarActivity context;
    VaultsAdapter adapter;
    OnVaultSelectedListener mOnVaultSelected;
    OnFragmentFinishListener mFinishListener;
    private boolean isPaused = false;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnVaultSelected = (OnVaultSelectedListener) activity;
            mFinishListener = (OnFragmentFinishListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @UiThread
    @AfterViews
    void oncreate() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        context = (ActionBarActivity) getActivity();
        VaultHolder.getInstance().clear();
        if (mLinearView != null)
            mLinearView.removeAllViews();
        context.getSupportActionBar().setTitle(R.string.App__name);
        java.io.File root = Storage.getRoot();
        if (!Util.canWrite(root)) {
            Util.alert(CustomApp.context,
                    CustomApp.context.getString(R.string.Error__root_IOException),
                    CustomApp.context.getString(R.string.Error__root_IOException_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mFinishListener.onNew(null, new SettingsFragment_());
                        }
                    },
                    null
            );
            return;
        }
        adapter = new VaultsAdapter(context, null);
        ArrayList<File> files = Storage.getDirectories(root);
        for (int i = 0; i < files.size(); i++) {
            adapter.add(files.get(i).getName());
            final View mView = adapter.getView(i, mLinearView); //inject vaults into list
            mLinearView.addView(mView, i);
            setClickListener(mView, i);
        }
        if (adapter.getCount() == 0) {
            nothing.setVisibility(View.VISIBLE);
            mLinearView.setVisibility(View.GONE);
        } else {
            nothing.setVisibility(View.GONE);
            mLinearView.setVisibility(View.VISIBLE);
        }
        showTutorial();
    }

    public void onEventMainThread(FilesActivity.shouldRefresh ignored) {
        oncreate();
    }

    public void setClickListener(final View mView, final int i) {

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                open(adapter.getItem(i), mView, i);
            }
        });
        mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                switchView(mView, R.id.vault_rename_layout);
                ((EditText) mView.findViewById(R.id.rename_name)).setText(adapter.getItem(i));
                mView.findViewById(R.id.rename_ok)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View ignored) {
                                final String newName = ((EditText) mView.findViewById(R.id.rename_name))
                                        .getText().toString();
                                switchView(mView, R.id.vault_decrypt_layout);
                                mView.findViewById(R.id.open_ok)
                                        .setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View ignored) {
                                                String password = ((EditText) mView.findViewById(R.id.open_password))
                                                        .getText().toString();
                                                rename(i, newName, password);
                                                switchView(mView, R.id.vault_decrypt_layout);
                                            }
                                        });
                                mView.findViewById(R.id.open_cancel)
                                        .setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View ignored) {
                                                switchView(mView, R.id.vault_name_layout);
                                            }
                                        });
                            }
                        });
                mView.findViewById(R.id.rename_cancel)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                switchView(mView, R.id.vault_name_layout);
                            }
                        });
                return true;
            }
        });
    }

    @OptionsItem(R.id.action_add_vault)
    void add() {
        final View dialogView = View.inflate(context, R.layout.new_credentials, null);
        final EditText password = new EditText(context);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Vault__new))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = ((EditText) dialogView.findViewById(R.id.newName)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.stealth_keycode)).getText().toString();
                        String Confirmpassword = ((EditText) dialogView.findViewById(R.id.confirmPassword)).getText().toString();
                        File directory = new File(Storage.getRoot().getAbsolutePath() + "/" + name);
                        if (!password.equals(Confirmpassword) || "".equals(password))
                            passwordWrong();
                        else if (directory.mkdirs()) {
                            // Create vault to initialize the vault header
                            ProgressDialog progress = new ProgressDialog(context);
                            progress.setIndeterminate(true);
                            progress.setMessage(getString(R.string.Vault__initializing));
                            progress.show();
                            createVaultInBackground(name, password, directory, dialog, progress);
                        } else
                            failedtocreate();

                    }
                }).setNegativeButton(getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    @Background
    void createVaultInBackground(String name, String password, File directory, DialogInterface dialog, ProgressDialog progressDialog) {
        VaultHolder.getInstance().createAndRetrieveVault(name, password);
        try {
            File file = new File(directory + "/.nomedia");
            file.delete();
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(name.getBytes());
            outputStream.close();
            oncreate();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.dismiss();
        progressDialog.dismiss();
    }

    @UiThread
    void passwordWrong() {
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Error__wrong_password_confirmation))
                .setMessage(getString(R.string.Error__wrong_password_confirmation_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    @UiThread
    void failedtocreate() {
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Error__cannot_create_vault))
                .setMessage(getString(R.string.Error__cannot_create_vault_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    @OptionsItem(R.id.action_restore)
    void restore() {
        ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
        INCLUDE_EXTENSIONS_LIST.add(".zip");

        Intent intent = new Intent(context, FileChooserActivity.class);

        intent.putStringArrayListExtra(
                FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                INCLUDE_EXTENSIONS_LIST);
        intent.putExtra(FileChooserActivity.PATH, Storage.getRoot().getAbsolutePath());
        startActivityForResult(intent, REQUESTCODE);
    }

    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUESTCODE:
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        final String path = FileUtils.getPath(context, uri);
                        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        mBuilder = new NotificationCompat.Builder(context);
                        mBuilder.setContentTitle(CustomApp.context.getString(R.string.Restore__title))
                                .setContentText(CustomApp.context.getString(R.string.Restore__in_progress))
                                .setSmallIcon(R.drawable.ic_stat_alert)
                                .setOngoing(true);
                        mBuilder.setProgress(0, 0, true);
                        mNotifyManager.notify(REQUESTCODE, mBuilder.build());
                        CustomApp.jobManager.addJobInBackground(new RestoreJob(context, new File(path)));
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onEventMainThread(RestoreDoneEvent event) {
        mBuilder.setProgress(0, 0, false)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(    //For long long text
                        String.format(CustomApp.context.getString(R.string.Restore__finish), event.backupFile)))
                .setOngoing(false);
        mNotifyManager.notify(REQUESTCODE, mBuilder.build());
    }

    public void onEventMainThread(RestoringFileEvent event) {
        mBuilder.setContentText(event.restoredFile.getAbsolutePath());
        mNotifyManager.notify(REQUESTCODE, mBuilder.build());
    }

    void open(final String vault, final View mView, final int i) {
        // vault name
        // View of lisitem
        // position of listitem in list
        switchView(mView, R.id.vault_decrypt_layout);
        mView.findViewById(R.id.open_ok)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String value = ((EditText) mView.findViewById(R.id.open_password))
                                .getText().toString();
                        mOnVaultSelected.onVaultSelected(vault, value);
                    }
                });
        mView.findViewById(R.id.open_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switchView(mView, R.id.vault_name_layout);
                    }
                });
    }

    void rename(final int position, final String newName, final String password) {
        Vault newVault = VaultHolder.getInstance().createAndRetrieveVault(
                adapter.getItem(position), password)
                .rename(newName);
        if (newVault == null)
            Util.alert(context,
                    getString(R.string.Error__rename_password_incorrect),
                    getString(R.string.Error__rename_password_incorrect_message),
                    Util.emptyClickListener,
                    null
            );
        oncreate();
    }

    @UiThread
    void switchView(final View parentView, int showView) {
        EditText passwordView = (EditText) parentView.findViewById(R.id.open_password);
        View renameView = parentView.findViewById(R.id.rename_name);
        ViewAnimator viewAnimator = (ViewAnimator) parentView.findViewById(R.id.viewAnimator);
        viewAnimator.setInAnimation(context, R.anim.slide_down);
        int viewIndex = 0;
        switch (showView) {
            case R.id.vault_name_layout:
                viewIndex = 0;
                break;
            case R.id.vault_decrypt_layout:
                viewIndex = 1;
                if (passwordView != null) {
                    passwordView.requestFocus();
                    passwordView.setText("");                               //Reset password field everytime
                }
                break;
            case R.id.vault_delete_layout:
                viewIndex = 2;
                break;
            case R.id.vault_rename_layout:
                viewIndex = 3;
                if (renameView != null)
                    renameView.requestFocus();
                break;
        }
        viewAnimator.setDisplayedChild(viewIndex);
    }

    void finish() {
        mFinishListener.onFinish(this);
    }

    void showTutorial() {
        if ((adapter.getCount() > 0) && (Pref.showVaultLongPressRenameTutorial().get())) {
            final View mView =
                    context.getLayoutInflater().inflate(R.layout.vault_item_tutorial, mLinearView, false);
            TextView mInstructions = (TextView) mView.findViewById(R.id.Tutorial__instruction);
            if (mInstructions != null)
                mInstructions.setText(R.string.Tutorial__long_click_to_rename);
            mLinearView.addView(mView, 0);
            mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mLinearView.removeView(mView);
                    Pref.edit()
                            .showVaultLongPressRenameTutorial()
                            .put(false)
                            .apply();
                    return true;
                }
            });
            return;                 //Show only one tutorial at a time. Don't overload users!!
        }
    }

    public interface OnVaultSelectedListener {
        public void onVaultSelected(String vault, String password);
    }

    public interface OnFragmentFinishListener {
        public void onFinish(Fragment fragment);

        public void onNew(Bundle bundle, Fragment fragment);
    }

    public interface onPanic {
        void onPanic();
    }
}
