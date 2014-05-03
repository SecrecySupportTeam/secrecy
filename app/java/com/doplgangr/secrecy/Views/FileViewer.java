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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.EmptyListener;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.DecryptFileProvider;
import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.FileSystem.FileObserver;
import com.doplgangr.secrecy.FileSystem.OurFileProvider;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;

import java.util.ArrayList;
import java.util.List;

@EActivity(R.layout.activity_file_viewer)
public class FileViewer extends ActionBarActivity {


    Activity context = this;


    @AfterInject
    void onCreate() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle(storage.getRoot().getAbsolutePath());
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.open_file))
                .setMessage(getString(R.string.open_file_message))
                .setView(input)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = input.getText().toString();
                        Uri file = getIntent().getData();
                        decrypt(new File(new java.io.File(file.getPath()), password), null, null);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                paused();
            }
        }).show();
    }

    @Background
    void decrypt(File file, final ProgressBar pBar, final EmptyListener onFinish) {
        java.io.File tempFile = getFile(file, pBar, onFinish);
        if (tempFile != null) {
            if (tempFile.getParentFile().equals(storage.getTempFolder())) {
                java.io.File newFile = new java.io.File(storage.getTempFolder(), tempFile.getName());
                tempFile = newFile;
            }
            Uri uri = OurFileProvider.getUriForFile(context, DecryptFileProvider.class.getName(), tempFile);
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
            String mimeType = myMime.getMimeTypeFromExtension(file.getType());
            newIntent.setDataAndType(uri, mimeType);
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //altIntent: resort to using file provider when content provider does not work.
            Intent altIntent = new Intent(android.content.Intent.ACTION_VIEW);
            Uri rawuri = Uri.fromFile(tempFile);
            altIntent.setDataAndType(rawuri, mimeType);
            List<Intent> targetedShareIntents = new ArrayList<Intent>();
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(newIntent, PackageManager.MATCH_DEFAULT_ONLY);
            //for (ResolveInfo info : resInfoList) {
            //Intent share = new Intent(android.content.Intent.ACTION_VIEW);
            //share.setDataAndType(uri, mimeType);
            //if (!info.activityInfo.packageName.equalsIgnoreCase("com.doplgangr.secrecy")) {
            // String packageName = info.activityInfo.packageName;
            //context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //share.setPackage(packageName);
            //targetedShareIntents.add(share);
            //}
            //}
            //Intent chooserIntent = Intent.createChooser(newIntent,
            // getString(R.string.view_file_dialog));
            afterDecrypt(newIntent, altIntent);
            Intent i = new Intent(context, FileObserver.class);
            i.putExtra(Config.file_extra, tempFile.getAbsolutePath());
            context.startService(i);
        }

    }

    public java.io.File getFile(final File file, final ProgressBar pBar, final EmptyListener onfinish) {
        CryptStateListener listener = new CryptStateListener() {
            @Override
            public void updateProgress(int progress) {
                updatePBar(pBar, progress);
            }

            @Override
            public void setMax(int max) {
                maxPBar(pBar, max);
            }

            @Override
            public void onFailed(int statCode) {
                String message;
                switch (statCode) {
                    case Config.wrong_password:
                        message = getString(R.string.error_wrong_password);
                        break;
                    case Config.file_not_found:
                        message = getString(R.string.error_file_not_found);
                        break;
                    default:
                        message = getString(R.string.error_unknown);
                }
                alert(message);
            }

            @Override
            public void Finished() {
                onfinish.run();
            }
        };
        return file.readFile(listener);
    }

    @UiThread
    void afterDecrypt(Intent newIntent, Intent altIntent) {
        //startFileOb
        try {
            startActivity(newIntent);
            paused();
        } catch (android.content.ActivityNotFoundException e) {
            try {
                startActivity(altIntent);
                paused();
            } catch (android.content.ActivityNotFoundException e2) {
                Util.toast(this, getString(R.string.error_no_activity_view), Toast.LENGTH_LONG);
            }
        }
    }

    @UiThread
    void alert(String message) {
        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                paused();
            }
        };
        Util.alert(this, getString(R.string.error_decrypt_dialog), message, click, null);
    }

    @UiThread
    void updatePBar(ProgressBar pBar, int progress) {
        if (pBar != null)
            pBar.setProgress(progress);
    }

    @UiThread
    void maxPBar(ProgressBar pBar, int max) {
        if (pBar != null)
            pBar.setMax(max);
    }

    void paused() {
    }

    @Override
    public void onDestroy() {
        Intent i = new Intent(context, FileObserver.class);
        context.stopService(i);
        super.onDestroy();
    }
}
