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

package com.doplgangr.secrecy.UpdateManager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.TextView;

import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.VaultsListFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.io.FileUtils;

import java.io.IOException;

/*
Called whenever user installs/upgrades.
context class is for handling the differences in vault versions.
The changes are hardcoded in the respective "transition actions"
e.g. from version 4 to version 5
The manager shows what it is doing and allows user to proceed when update is finished.
 */
@EFragment(R.layout.activity_update_manager)
public class UpdateManager extends Fragment {
    //Console-like log view
    @ViewById(R.id.log)
    TextView log;

    //Continue button
    @ViewById(R.id.continueButton)
    Button continueButton;

    //Preference that stores last app version
    @Pref
    AppVersion_ version;
    @Pref
    Prefs_ Prefs;
    VaultsListFragment.OnFragmentFinishListener mFinishListener;
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

    @AfterViews
    void onCreate() {
        context = getActivity();
        // Fills variables with appropriate version data
        getVersionInfo();

        // If the version is not upgraded, skip the update manager
        if (versionnow == version.no().get()) {
            finish();
            return;
        }

        // Writes the previous/current version into the log view
        appendlog(String.format(getString(R.string.Updater__previous_version), version.no().get(), version.name().get()));
        appendlog(String.format(getString(R.string.Updater__next_version), versionnow, versionnow_name));

        // Switches between different upgrades, based on last app version.
        switch (version.no().get()) {
            case 22:
                version21to22();
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
        }

        // Notify user that app is upgrading
        appendlog(getString(R.string.Updater__updating));
    }

    @Background
    void version21to22() {
        //Nahh
        onFinishAllUpgrade();
    }

    @Background
    void version20to21() {
        //Nahh
        version21to22();
    }


    @Background
    void version19to20() {
        //Nahh
        version20to21();
    }

    @Background
    void version18to19() {
        //Nahh
        version19to20();
    }

    @Background
    void version17to18() {
        if (Prefs.OpenPIN().exists())           // version 18 adds option to enable/disable stealth.
            Prefs.stealth().put(true);          // enable stealth if is set prior to 18.
        version18to19();
    }

    @Background
    void version16to17() {
        //Still nothing...
        version17to18();
    }

    @Background
    void version15to16() {
        //Still nothing...
        version16to17();
    }

    @Background
    void version14to15() {
        //Still nothing...
        version15to16();
    }

    @Background
    void version13to14() {
        //Still nothing...
        version14to15();
    }

    @Background
    void version12to13() {
        //Nothing to upgrade
        version13to14();
    }

    @Background
    void version11to12() {
        //Nothing to upgrade
        version12to13();
    }


    @Background
    void version10to11() {
        //Nothing to upgrade
        version11to12();
    }


    @Background
    void version9to10() {
        //Nothing to upgrade
        version10to11();
    }

    @Background
    void version8to9() {
        // Changes filebase path to new format.
        if (!Util.canWrite(storage.getRoot())) {
            //Append with sdcard link
            appendlog("\nOld sdCard format. Changing to new format.");
            String newRoot = Environment.getExternalStorageDirectory()
                    .getAbsoluteFile()
                    + "/" + storage.getRoot().getAbsolutePath();
            storage.setRoot(newRoot);
        }
        version9to10();
    }

    @Background
    void version7to8() {
        //Nothing to upgrade
        version8to9();
    }

    @Background
    void version6to7() {
        // Fix a bug in version 6 that corrupts the vault if user tries to move it elsewhere

        if (!storage.getRoot().getName().equals("SECRECYFILES")) {
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
                org.apache.commons.io.FileUtils.copyDirectory(getRoot(), storage.getRoot());
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

    @Background
    void version5to6() {
        //Nothing to upgrade
        version6to7();
    }

    @Background
    void version3to5() {
        //Nothing to upgrade
        version5to6();
    }

    @Background
    void version2to3() {
        //Nothing to upgrade
        version3to5();
    }

    @Background
    void version1to2() {
        // remove the temp folder, we do not use it anymore.
        java.io.File root = storage.getRoot();
        if (Util.canWrite(root)) {
            java.io.File[] files = root.listFiles();
            for (java.io.File inFile : files)
                if (inFile.isDirectory())
                    if ("TEMP".equals(inFile.getName()))
                        storage.DeleteRecursive(inFile);
            appendlog(getString(R.string.Updater__one_to_two));
        }
        version2to3();
    }

    @UiThread
    void onFinishAllUpgrade() {
        // When all updates are finished, notify user and enable the continue button
        appendlog(getString(R.string.Updater__update_finish));
        continueButton.setEnabled(true);

        // Write the new version info into the preferences, for next upgrades.
        version.edit()
                .no()
                .put(versionnow)
                .name()
                .put(versionnow_name)
                .apply();
    }

    @UiThread
    void appendlog(String message) {
        // Appends log info to log view
        log.append(message);
    }

    @Click(R.id.continueButton)
    public void Continue() {
        // When user presses the continue button, alert if app is still in beta.

        //Define 100 as initial beta code
        if (versionnow < 100)
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
            //Define 200 as initial official release code
        else if (versionnow < 200)
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
        else
            finish();
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
    }

}