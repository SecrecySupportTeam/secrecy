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

package com.doplgangr.secrecy.updatemanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.filesystem.Base64Coder;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.views.VaultsListFragment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/*
Called whenever user installs/upgrades.
context class is for handling the differences in vault versions.
The changes are hardcoded in the respective "transition actions"
e.g. from version 4 to version 5
The manager shows what it is doing and allows user to proceed when update is finished.
 */
public class UpdateManager extends Fragment {
    //Console-like log view
    private TextView log;

    //Continue button
    private Button continueButton;

    //Preference that stores last app version
    int version;
    private VaultsListFragment.OnFragmentFinishListener mFinishListener;
    //Current version
    private Integer versionnow;
    private String versionnow_name;
    private Context context;

    private static java.io.File getRoot() {
        // Function for determining temp folder location in version1, only used in upgrading from v1 to v2
        java.io.File sdCard = Environment.getExternalStorageDirectory();
        java.io.File tempDir = new java.io.File(sdCard.getAbsolutePath() + "/" + "SECRECYFILES");
        tempDir.mkdirs();
        return tempDir;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        // Fills variables with appropriate version data
        getVersionInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_update_manager, container, false);

        log = (TextView) view.findViewById(R.id.log);
        continueButton = (Button) view.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When user presses the continue button, alert if app is still in beta.

                //Define 100 as initial beta code
                if (versionnow < 100) {
                    Util.alert(context,
                            getString(R.string.Updater__alpha),
                            getString(R.string.Updater__alpha_message),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            },
                            null
                    );
                } //Define 200 as initial official release code
                else if (versionnow < 200) {
                    Util.alert(context,
                            getString(R.string.Updater__beta),
                            getString(R.string.Updater__beta_message),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            },
                            null
                    );
                }
                else {
                    finish();
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        version = PreferenceManager.getDefaultSharedPreferences(context).getInt(Config.APP_VERSION_NUMBER, 1);
        String versionName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Config.APP_VERSION_NAME, getString(R.string.Updater__alpha0_1));

        // If the version is not upgraded, skip the update manager
        if (versionnow == version) {
            finish();
            return;
        }

        // Writes the previous/current version into the log view
        appendlog(String.format(getString(R.string.Updater__previous_version), version, versionName));
        appendlog(String.format(getString(R.string.Updater__next_version), versionnow, versionnow_name));

        // Switches between different upgrades, based on last app version.
        switch (version) {
            case 32:
                version32to40();
            case 31:
                version31to32();
                break;
            case 30:
                version30to31();
                break;
            case 21:
                version21to30();
                break;
            case 20:
                version20to21();
                break;
            case 19:
                version19to20();
                break;
            case 18:
                version18to19();
                break;
            case 17:
                version17to18();
                break;
            case 16:
                version16to17();
                break;
            case 15:
                version15to16();
                break;
            case 14:
                version14to15();
                break;
            case 13:
                version13to14();
                break;
            case 12:
                version12to13();
                break;
            case 11:
                version11to12();
                break;
            case 10:
                version10to11();
                break;
            case 9:
                version9to10();
                break;
            case 8:
                version8to9();
                break;
            case 7:
                version7to8();
                break;
            case 6:
                version6to7();
                break;
            case 5:
                version5to6();
                break;
            case 3:
                version3to5();
                break;
            case 2:
                version2to3();
                break;
            case 1:
                version1to2();
                break;
            default:
                onFinishAllUpgrade();   //FailSafe
        }

        // Notify user that app is upgrading
        appendlog(getString(R.string.Updater__updating));
    }

    void version32to40() {
        Collection folders = FileUtils.listFilesAndDirs(Storage.getRoot(), FalseFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (Object folderObject : folders) { //Search for dirs in root
            File folder = (File) folderObject;
            if (new File(folder, ".vault").exists() || !new File(folder, ".nomedia").exists()) {
                appendlog("\n" + folder.getAbsolutePath() + " is 5.x or not a vault, skip");
                continue; //The whole thing should be skipped because vault is in 5.x standard.
            }
            appendlog("\n" + folder.getAbsolutePath() + " is pre-5.x");
            Collection files = FileUtils.listFiles(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            //walks the whole file tree, find out files that do not have encoded file names
            //and encode them.
            for (Object file : files) {
                File realFile = (File) file;
                if (".nomedia".equals(realFile.getName()))
                    continue;
                String fileName = FilenameUtils.removeExtension(realFile.getName());
                fileName = fileName.replace("_thumb", "");
                try {
                    Base64Coder.decodeString(fileName);
                } catch (IllegalArgumentException e) {
                    String encodedFileName = Base64Coder.encodeString(fileName);
                    fileName = realFile.getAbsolutePath().replace(fileName, encodedFileName);
                    realFile.renameTo(new File(fileName));
                }
            }
        }
        onFinishAllUpgrade();
    }

    void version31to32() {
        //Nahh
        version32to40();
    }

    void version30to31() {
        //Nahh
        version31to32();
    }

    void version21to30() {
        //Nahh
        version30to31();
    }

    void version20to21() {
        //Nahh
        version21to30();
    }

    void version19to20() {
        //Nahh
        version20to21();
    }

    void version18to19() {
        //Nahh
        version19to20();
    }

    void version17to18() {
        // version 18 adds option to enable/disable stealth.
        // enable stealth if is set prior to 18.
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Config.STEALTH_MODE_PASSWORD, "").equals("")) {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(context).edit();
            editor.putBoolean(Config.STEALTH_MODE, true);
            editor.apply();
        }
        version18to19();
    }

    void version16to17() {
        //Still nothing...
        version17to18();
    }

    void version15to16() {
        //Still nothing...
        version16to17();
    }

    void version14to15() {
        //Still nothing...
        version15to16();
    }

    void version13to14() {
        //Still nothing...
        version14to15();
    }

    void version12to13() {
        //Nothing to upgrade
        version13to14();
    }

    void version11to12() {
        //Nothing to upgrade
        version12to13();
    }

    void version10to11() {
        //Nothing to upgrade
        version11to12();
    }

    void version9to10() {
        //Nothing to upgrade
        version10to11();
    }

    void version8to9() {
        // Changes filebase path to new format.
        if (!Util.canWrite(Storage.getRoot())) {
            //Append with sdcard link
            appendlog("\nOld sdCard format. Changing to new format.");
            String newRoot = Environment.getExternalStorageDirectory()
                    .getAbsoluteFile()
                    + "/" + Storage.getRoot().getAbsolutePath();
            Storage.setRoot(newRoot);
        }
        version9to10();
    }

    void version7to8() {
        //Nothing to upgrade
        version8to9();
    }

    void version6to7() {
        // Fix a bug in version 6 that corrupts the vault if user tries to move it elsewhere

        if (!Storage.getRoot().getName().equals("SECRECYFILES")) {
            appendlog("\nUser have used v6 and moved vaults elsewhere");
            Util.alert(context,
                    "Upgrading from alpha 0.6 to 0.7",
                    "We detect that you have moved your vaults using version 0.6." +
                            " We discovered some bugs and we will try to fix, if any, problems associated" +
                            " with it...",
                    Util.emptyClickListener,
                    null
            );
            appendlog("\nTrying to move again...");
            try {
                org.apache.commons.io.FileUtils.copyDirectory(getRoot(), Storage.getRoot());
                appendlog("\nFinish re-moving vaults");
                FileUtils.deleteDirectory(getRoot());
            } catch (IOException E) {
                E.printStackTrace();
                Util.alert(context,
                        "Error moving vaults",
                        "We encountered an error. Please contact developer for help (mkcyyin(at)gmail.com). Updating aborted.",
                        Util.emptyClickListener,
                        null);
                return;
            }

        }
        version7to8();
    }

    void version5to6() {
        //Nothing to upgrade
        version6to7();
    }

    void version3to5() {
        //Nothing to upgrade
        version5to6();
    }

    void version2to3() {
        //Nothing to upgrade
        version3to5();
    }

    void version1to2() {
        // remove the temp folder, we do not use it anymore.
        java.io.File root = Storage.getRoot();
        if (Util.canWrite(root)) {
            java.io.File[] files = root.listFiles();
            for (java.io.File inFile : files)
                if (inFile.isDirectory())
                    if ("TEMP".equals(inFile.getName()))
                        Storage.DeleteRecursive(inFile);
            appendlog(getString(R.string.Updater__one_to_two));
        }
        version2to3();
    }

    void onFinishAllUpgrade() {
        // When all updates are finished, notify user and enable the continue button
        appendlog(getString(R.string.Updater__update_finish));
        continueButton.setEnabled(true);

        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(Config.APP_VERSION_NUMBER, versionnow);
        editor.putString(Config.APP_VERSION_NAME, versionnow_name);
        editor.apply();
    }

    void appendlog(String message) {
        // Appends log info to log view
        log.append(message);
    }

    void getVersionInfo() {
        // get version info from the package manager
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Util.log("Cannot get package info, abort.");
            finish();
            return;
        }
        versionnow = pInfo.versionCode;
        versionnow_name = pInfo.versionName;
    }

    void finish() {
        mFinishListener.onFinish(this);
        mFinishListener.onNew(null, new VaultsListFragment());
    }
}