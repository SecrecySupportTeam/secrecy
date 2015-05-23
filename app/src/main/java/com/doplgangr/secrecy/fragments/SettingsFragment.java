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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.activities.FileChooserActivity;
import com.doplgangr.secrecy.utils.Util;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.premium.PremiumStateHelper;
import com.doplgangr.secrecy.premium.StealthMode;
import com.doplgangr.secrecy.adapters.VaultsListFragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragment{
    private static final int REQUEST_CODE_SET_VAULT_ROOT = 6384;
    private static final int REQUEST_CODE_MOVE_VAULT = 2058;
    private Context context;
    private VaultsListFragment.OnFragmentFinishListener mFinishListener;

    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<>();
    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }

    private String stealth_mode_message;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        this.context = getActivity();

        Resources res = getResources();
        stealth_mode_message = getString(R.string.Settings__stealth_mode_message);

        preparePreferenceStealthMode();
        preparePreferenceStealthModePassword();
        preparePreferenceMaxImageSize();
        preparePreferenceVaultRoot();
        preparePreferenceVaultMove();
    }

    private void preparePreferenceStealthMode(){
        final CheckBoxPreference stealth_mode = (CheckBoxPreference) findPreference(Config.STEALTH_MODE);
        stealth_mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

                if (!(Boolean) o) {
                    StealthMode.showApp(context);

                    editor.putBoolean(Config.STEALTH_MODE, (Boolean) o);
                    editor.putString(Config.STEALTH_MODE_PASSWORD, "");
                    editor.apply();
                } else {
                    editor.putBoolean(Config.STEALTH_MODE, (Boolean) o);
                    editor.apply();
                }
                return true;
            }
        });
    }

    private void preparePreferenceStealthModePassword(){
        final Preference stealth_mode_password = findPreference(Config.STEALTH_MODE_PASSWORD);
        String openPin = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Config.STEALTH_MODE_PASSWORD, "");
        if (!openPin.equals("")) {
            stealth_mode_password.setSummary("*# " + openPin);
        }

        PremiumStateHelper.PremiumListener mPremiumListener = new PremiumStateHelper.PremiumListener() {
            @Override
            public void isPremium() {
                stealth_mode_password.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                            Util.alert(context,
                                    getString(R.string.Stealth__no_telephony),
                                    getString(R.string.Stealth__no_telephony_message),
                                    Util.emptyClickListener,
                                    null);
                            return true;
                        }
                        final View dialogView = View.inflate(context, R.layout.dialog_stealth, null);
                        new AlertDialog.Builder(context)
                                .setMessage(context.getString(R.string.Settings__stealth_explanation))
                                .setView(dialogView)
                                .setInverseBackgroundForced(true)
                                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String password = ((EditText) dialogView.
                                                findViewById(R.id.stealth_keycode))
                                                .getText().toString();
                                        SharedPreferences.Editor editor =
                                                PreferenceManager.getDefaultSharedPreferences(context).edit();
                                        editor.putString(Config.STEALTH_MODE_PASSWORD, password);
                                        editor.apply();
                                        confirm_stealth(password);
                                    }
                                })
                                .setNegativeButton(getString(R.string.CANCEL), Util.emptyClickListener)
                                .show();
                        return true;
                    }
                });
            }
                @Override
                public void notPremium() {
                    stealth_mode_password.setSummary(stealth_mode_message + " "
                            + context.getString(R.string.Settings__only_permium));
                    stealth_mode_password.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            mFinishListener.onNew(null, new PremiumFragment()); //Switch fragment to donation
                            return true;
                        }
                    });
                }
            };
            new PremiumStateHelper(getActivity(), mPremiumListener);
    }

    private void preparePreferenceMaxImageSize(){
        Preference image_size = findPreference(Config.IMAGE_SIZE);
        image_size.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Util.loadSelectedImageSize((String) o);
                return true;
            }
        });
    }

    private void preparePreferenceVaultRoot(){
        Preference vault_root = findPreference("vault_root");
        vault_root.setSummary(Storage.getRoot().getAbsolutePath());
        vault_root.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                choosePath();
                return true;
            }
        });
    }

    private void preparePreferenceVaultMove(){
        Preference vault_move = findPreference("vault_move");
        vault_move.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                movePath();
                return true;
            }
        });
    }

    private void confirm_stealth(String password) {
        final View dialogView = View.inflate(context, R.layout.dialog_confirm_stealth, null);
        ((TextView) dialogView
                .findViewById(R.id.stealth_keycode))
                .append(password);
        new AlertDialog.Builder(context)
                .setInverseBackgroundForced(true)
                .setView(dialogView)
                .setMessage(R.string.Settings__try_once_before_hide)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor
                                = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.putBoolean(Config.SHOW_STEALTH_MODE_TUTORIAL, true);
                        editor.apply();
                        Intent dial = new Intent();
                        dial.setAction("android.intent.action.DIAL");
                        dial.setData(Uri.parse("tel:"));
                        dial.setFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(dial);
                        getActivity().finish();
                    }
                })
                .show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((ActionBarActivity) getActivity()).getSupportActionBar()
                .setTitle(R.string.Page_header__settings);

        return inflater.inflate(R.layout.activity_settings, container, false);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SET_VAULT_ROOT:
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the selected file
                        final File file = (File) data.getSerializableExtra(FileChooserActivity.FILE_SELECTED);
                        try {
                            Storage.setRoot(file.getAbsolutePath());
                            Preference vault_root = findPreference(Config.VAULT_ROOT);
                            vault_root.setSummary(Storage.getRoot().getAbsolutePath());
                        } catch (Exception e) {
                            Log.e("SettingsFragment", "File select error", e);
                        }
                    }
                }
                break;
            case REQUEST_CODE_MOVE_VAULT:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the selected file
                        final File file = (File) data.getSerializableExtra(FileChooserActivity.FILE_SELECTED);
                        try {
                            final String path = file.getAbsolutePath();
                            if (path.contains(Storage.getRoot().getAbsolutePath())) {
                                Util.alert(context,
                                        getString(R.string.Settings__cannot_move_vault),
                                        getString(R.string.Settings__cannot_move_vault_message),
                                        Util.emptyClickListener,
                                        null);
                                break;
                            }
                            Util.alert(context,getString(R.string.Settings__move_vault),
                                    String.format(getString(R.string.move_message), Storage.getRoot().getAbsolutePath(), path),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String[] children = new File(path).list();
                                            if (children.length == 0) {
                                                final ProgressDialog progressDialog = ProgressDialog.show(context, null,
                                                        context.getString(R.string.Settings__moving_vault), true);
                                                new Thread(new Runnable() {
                                                    public void run() {
                                                        moveStorageRoot(path, progressDialog);
                                                    }
                                                }).start();
                                            } else
                                                Util.alert(context,
                                                        getString(R.string.Error__files_exist),
                                                        getString(R.string.Error__files_exist_message),
                                                        Util.emptyClickListener,
                                                        null
                                                );
                                        }
                                    },
                                    Util.emptyClickListener
                            );
                        } catch (Exception e) {
                            Log.e("SettingsFragment", "File select error", e);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void moveStorageRoot(String path, ProgressDialog progressDialog) {
        File oldRoot = Storage.getRoot();
        try {
            org.apache.commons.io.FileUtils.copyDirectory(oldRoot, new File(path));
            Storage.setRoot(path);
            Preference vault_root = findPreference(Config.VAULT_ROOT);
            vault_root.setSummary(Storage.getRoot().getAbsolutePath());
            Util.toast(getActivity(),
                    String.format(getString(R.string.Settings__moved_vault), path), Toast.LENGTH_LONG);
        } catch (Exception E) {
            Util.alert(context,
                    context.getString(R.string.Error__moving_vault),
                    context.getString(R.string.Error__moving_vault_message),
                    Util.emptyClickListener,
                    null);
            progressDialog.dismiss();
            return;
        }
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(oldRoot);
        } catch (IOException ignored) {
            //ignore
        }
        progressDialog.dismiss();
    }

    void choosePath() {
        Intent intent = new Intent(getActivity(), FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.FOLDERS_ONLY, true);
        startActivityForResult(intent, REQUEST_CODE_SET_VAULT_ROOT);
    }

    void movePath() {
        Intent intent = new Intent(getActivity(), FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.FOLDERS_ONLY, true);
        startActivityForResult(intent, REQUEST_CODE_MOVE_VAULT);
    }


}