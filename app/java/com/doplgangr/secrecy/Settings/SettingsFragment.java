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

package com.doplgangr.secrecy.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.support.v4.content.IntentCompat;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp_;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Premium.PremiumActivity_;
import com.doplgangr.secrecy.Premium.PremiumStateHelper;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Matthew on 2/23/14.
 */
@EFragment
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String TAG = "PREFERENCEFRAGMENT";
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final int REQUEST_CODE_2 = 2058; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();

    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }

    @StringRes(R.string.on_changed_alert)
    String alert;
    @StringRes(R.string.legal_long)
    String libraries;
    @Pref
    Prefs_ Prefs;

    @AfterViews
    void onCreate() {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        update();
    }

    @UiThread
    void update() {
        Preference pref = findPreference("version");
        pref.setSummary(CustomApp_.VERSIONNAME);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        Preference dialogPreference = getPreferenceScreen().findPreference("legal");
        dialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Util.alert(getActivity(),
                        null,
                        libraries,
                        Util.emptyClickListener,
                        null);
                return true;
            }
        });

        Preference vault_root = findPreference("vault_root");
        vault_root.setSummary(storage.getRoot().getAbsolutePath());
        vault_root.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                choosePath(new getFileListener() {
                    @Override
                    public void get(File file) {
                        Intent intent = new Intent(getActivity(), FileChooserActivity.class);
                        intent.putStringArrayListExtra(
                                FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                                INCLUDE_EXTENSIONS_LIST);
                        intent.putExtra(FileChooserActivity.PATH, file.getAbsolutePath());
                        intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
                return true;
            }
        });
        Preference vault_move = findPreference("vault_move");
        vault_move.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                choosePath(new getFileListener() {
                    @Override
                    public void get(File file) {
                        Intent intent = new Intent(getActivity(), FileChooserActivity.class);

                        intent.putStringArrayListExtra(
                                FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                                INCLUDE_EXTENSIONS_LIST);
                        intent.putExtra(FileChooserActivity.PATH, file.getAbsolutePath());
                        intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
                        startActivityForResult(intent, REQUEST_CODE_2);
                    }
                });
                return true;
            }
        });
        final Preference stealth_mode = findPreference("stealth_mode");
        PremiumStateHelper.PremiumListener mPremiumListener = new PremiumStateHelper.PremiumListener() {
            @Override
            public void isPremium() {
                stealth_mode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final View dialogView = View.inflate(getActivity(), R.layout.dialog_stealth, null);
                        new AlertDialog.Builder(getActivity())
                                .setMessage("In stealth mode, the app icon is hidden from the app launcher. The only way to enter the app is through dialing your secret code from the dialer. Please choose a secret code easy to remember:")
                                .setView(dialogView)
                                .setInverseBackgroundForced(true)
                                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String password = ((EditText) dialogView.
                                                findViewById(R.id.password))
                                                .getText().toString();
                                        Prefs.OpenPIN().put(password);
                                        confirm_stealth(password);
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), Util.emptyClickListener)
                                .show();
                        return true;
                    }
                });
            }

            @Override
            public void notPremium() {
                stealth_mode.setSummary(stealth_mode.getSummary() + " Only available to donate users");
                stealth_mode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(getActivity(), PremiumActivity_.class));
                        return true;
                    }
                });
            }
        };
        new PremiumStateHelper(getActivity(), mPremiumListener);

    }

    void confirm_stealth(String password) {
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_confirm_stealth, null);
        ((TextView) dialogView
                .findViewById(R.id.password))
                .append(password);
        new AlertDialog.Builder(getActivity())
                .setInverseBackgroundForced(true)
                .setView(dialogView)
                .setMessage("Before we go on and hide the app icon, we need to make sure you can launch it in stealth mode. Please go to the dialer and dial the secret code:")
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.stealthMode().put(-1);
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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        Util.toast(getActivity(), alert, Toast.LENGTH_LONG);
    }

    @Override
    @Background
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(getActivity(), uri);
                            storage.setRoot(path);
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }
                }
                update();
                break;
            case REQUEST_CODE_2:

                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(getActivity(), uri);
                            if (path.contains(storage.getRoot().getAbsolutePath())) {
                                Util.alert(getActivity(),
                                        getString(R.string.cannot_move),
                                        getString(R.string.cannot_move_message),
                                        Util.emptyClickListener,
                                        null);
                                break;
                            }
                            Util.alert(getActivity(),
                                    getString(R.string.move),
                                    String.format(getString(R.string.move_message), storage.getRoot().getAbsolutePath(), path),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String[] children = new File(path).list();
                                            if (children.length == 0) {
                                                final ProgressDialog progDailog = ProgressDialog.show(getActivity(), null,
                                                        "Moving. Please wait....", true);
                                                move(path, progDailog);
                                            } else
                                                Util.alert(getActivity(),
                                                        getString(R.string.error_have_things),
                                                        getString(R.string.have_things_message),
                                                        Util.emptyClickListener,
                                                        null
                                                );
                                        }
                                    },
                                    Util.emptyClickListener
                            );
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Background
    void move(String path, ProgressDialog progressDialog) {
        File oldRoot = storage.getRoot();
        try {
            org.apache.commons.io.FileUtils.copyDirectory(oldRoot, new File(path));
            storage.setRoot(path);
            Util.toast(getActivity(),
                    String.format(getString(R.string.moved), path), Toast.LENGTH_LONG);
            update();
        } catch (Exception E) {
            Util.alert(getActivity(),
                    "Error moving vaults",
                    "We encountered an error. Please try again later.",
                    Util.emptyClickListener,
                    null);
            progressDialog.dismiss();
            return;
        }
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(oldRoot);
        } catch (IOException e) {
            //ignore
        }
        progressDialog.dismiss();
    }

    @UiThread
    void choosePath(final getFileListener listener) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setTitle("Select Storage:");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.select_dialog_singlechoice);
        final Map<String, File> storages = Util.getAllStorageLocations();
        for (String key : storages.keySet())
            arrayAdapter.add(key);
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = arrayAdapter.getItem(which);
                        File file = storages.get(strName);
                        listener.get(file);
                    }
                }
        );
        builderSingle.show();
    }

    public interface getFileListener {
        void get(File file);
    }

}