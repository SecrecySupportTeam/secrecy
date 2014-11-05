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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.support.v4.content.IntentCompat;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp_;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Premium.PremiumFragment_;
import com.doplgangr.secrecy.Premium.PremiumStateHelper;
import com.doplgangr.secrecy.Premium.StealthMode;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.VaultsListFragment;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringArrayRes;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@EFragment
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String TAG = "PREFERENCEFRAGMENT";
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final int REQUEST_CODE_2 = 2058; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    ActionBarActivity context = null;
    @StringRes(R.string.Settings__stealth_mode_message)
    String stealth_mode_message;
    @StringArrayRes(R.array.Contributor__names)
    String[] contributorNames;
    @StringArrayRes(R.array.Contributor__description)
    String[] contributorDescription;
    @StringArrayRes(R.array.Contributor__links)
    String[] contributorLinks;

    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }

    @StringRes(R.string.Settings__changed_alert)
    String alert;
    @StringRes(R.string.Settings__libraries_message)
    String libraries;
    @Pref
    Prefs_ Prefs;
    VaultsListFragment.OnFragmentFinishListener mFinishListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    @AfterViews
    void onCreate() {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        context = (ActionBarActivity) getActivity();
        context.getSupportActionBar().setTitle(R.string.Page_header__settings);
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
                Util.alert(context,
                        null,
                        libraries,
                        Util.emptyClickListener,
                        null);
                return true;
            }
        });

        Preference vault_root = findPreference("vault_root");
        vault_root.setSummary(Storage.getRoot().getAbsolutePath());
        vault_root.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                choosePath(new getFileListener() {
                    @Override
                    public void get(File file) {
                        Intent intent = new Intent(context, FileChooserActivity.class);
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
        PreferenceGroup translatorList = (PreferenceGroup) findPreference("translators_list");
        for (int i = 0; i < contributorNames.length; i++) {
            Preference newPreference = new Preference(getActivity());
            newPreference.setTitle(contributorNames[i]);
            newPreference.setSummary(contributorDescription[i]);
            final int finali = i;
            newPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Uri uri = Uri.parse(contributorLinks[finali]);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
            translatorList.addPreference(newPreference);
        }
        CheckBoxPreference analytics = (CheckBoxPreference) findPreference("analytics");
        analytics.setChecked(Prefs.analytics().get());
        analytics.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Prefs.edit()
                        .analytics()
                        .put((Boolean) o)
                        .apply();
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
                        Intent intent = new Intent(context, FileChooserActivity.class);

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
        final CheckBoxPreference stealth_mode = (CheckBoxPreference) findPreference("stealth_mode");
        stealth_mode.setChecked(Prefs.stealth().get());
        stealth_mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!(Boolean) o) {
                    StealthMode.showApp(context);
                    Prefs.edit()
                            .stealth()
                            .put((Boolean) o)
                            .stealthMode()
                            .remove()
                            .OpenPIN()
                            .remove()
                            .apply();
                } else {
                    Prefs.edit()
                            .stealth()
                            .put((Boolean) o)
                            .apply();
                }
                return true;
            }
        });
        final Preference stealth_mode_password = findPreference("stealth_mode_password");
        if (Prefs.OpenPIN().exists())
            stealth_mode_password.setSummary("*# " + Prefs.OpenPIN().get());
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
                                        Prefs.OpenPIN().put(password);
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
                stealth_mode.setSummary(stealth_mode_message + context.getString(R.string.Settings__only_permium));
                stealth_mode_password.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        mFinishListener.onNew(null, new PremiumFragment_());    //Switch fragment to donation
                        return true;
                    }
                });
            }
        };
        new PremiumStateHelper(context, mPremiumListener);

    }

    void confirm_stealth(String password) {
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
                        Prefs.stealthMode().put(-1);
                        Intent dial = new Intent();
                        dial.setAction("android.intent.action.DIAL");
                        dial.setData(Uri.parse("tel:"));
                        dial.setFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(dial);
                        context.finish();
                    }
                })
                .show();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        Util.toast(context, alert, Toast.LENGTH_LONG);
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
                            final String path = FileUtils.getPath(context, uri);
                            Storage.setRoot(path);
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
                            final String path = FileUtils.getPath(context, uri);
                            if (path.contains(Storage.getRoot().getAbsolutePath())) {
                                Util.alert(context,
                                        getString(R.string.Settings__cannot_move_vault),
                                        getString(R.string.Settings__cannot_move_vault_message),
                                        Util.emptyClickListener,
                                        null);
                                break;
                            }
                            Util.alert(context,
                                    getString(R.string.Settings__move_vault),
                                    String.format(getString(R.string.move_message), Storage.getRoot().getAbsolutePath(), path),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String[] children = new File(path).list();
                                            if (children.length == 0) {
                                                final ProgressDialog progDailog = ProgressDialog.show(context, null,
                                                        context.getString(R.string.Settings__moving_vault), true);
                                                move(path, progDailog);
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
        File oldRoot = Storage.getRoot();
        try {
            org.apache.commons.io.FileUtils.copyDirectory(oldRoot, new File(path));
            Storage.setRoot(path);
            Util.toast(context,
                    String.format(getString(R.string.Settings__moved_vault), path), Toast.LENGTH_LONG);
            update();
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

    @UiThread
    void choosePath(final getFileListener listener) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle(context.getString(R.string.Settings__select_storage_title));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                context,
                R.layout.select_dialog_singlechoice);
        final Map<String, File> storages = Util.getAllStorageLocations();
        for (String key : storages.keySet())
            arrayAdapter.add(key);
        builderSingle.setNegativeButton(R.string.CANCEL,
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