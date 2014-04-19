package com.doplgangr.secrecy.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.doplgangr.secrecy.ChooseFolder;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.CustomApp_;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.storage;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Matthew on 2/23/14.
 */
@EFragment
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @StringRes(R.string.on_changed_alert)
    String alert;
    @StringRes(R.string.legal_long)
    String libraries;
    static final String TAG = "PREFERENCEFRAGMENT";

    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final int REQUEST_CODE_2 = 2058; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }


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
                new AlertDialog.Builder(getActivity())
                        .setMessage(libraries)
                        .setPositiveButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){
                            }
                        })
                        .show();
                return true;
            }
        });

        Preference vault_root = findPreference("vault_root");
        vault_root.setSummary(storage.getRoot().getAbsolutePath());
        vault_root.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), FileChooserActivity.class);

                intent.putStringArrayListExtra(
                        FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                        INCLUDE_EXTENSIONS_LIST);
                intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
                startActivityForResult(intent, REQUEST_CODE);
                return true;
            }
        });
        Preference vault_move = findPreference("vault_move");
        vault_move.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), FileChooserActivity.class);

                intent.putStringArrayListExtra(
                        FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                        INCLUDE_EXTENSIONS_LIST);
                intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
                startActivityForResult(intent, REQUEST_CODE_2);
                return true;
            }
        });
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        Toast.makeText(getActivity(), alert, Toast.LENGTH_LONG).show();
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
                            Toast.makeText(getActivity(),
                                    String.format(getString(R.string.folder_selected), path), Toast.LENGTH_LONG).show();
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
                                            if (children.length==0)
                                            try {
                                                moveDirectory(storage.getRoot(), new File(path));
                                                storage.setRoot(path);
                                                Toast.makeText(getActivity(),
                                                        String.format(getString(R.string.moved), path), Toast.LENGTH_LONG).show();
                                                update();
                                            }catch (Exception E){

                                            }
                                            else
                                                Util.alert(getActivity(),
                                                        getString(R.string.error_have_things),
                                                        getString(R.string.have_things_message),
                                                        Util.emptyClickListener,
                                                        null
                                                        );
                                        }
                                    },
                                    Util.emptyClickListener);
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }
                }

                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void moveDirectory(File sourceLocation, File targetLocation)
            throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists())
                targetLocation.mkdir();

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {
                moveDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            sourceLocation.renameTo(targetLocation);
        }

    }
}