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

package com.doplgangr.secrecy.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;
import com.doplgangr.secrecy.activities.FilesActivity;
import com.doplgangr.secrecy.filesystem.CryptStateListener;
import com.doplgangr.secrecy.filesystem.OurFileProvider;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.filesystem.files.SecrecyFile;
import com.doplgangr.secrecy.jobs.AddFileJob;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FileViewer extends Fragment {

    ActionBarActivity context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_file_viewer, container, false);
    }

    void addFileInBackground(final Vault secret, final Uri data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CustomApp.jobManager.addJobInBackground(new AddFileJob(context, secret, data));
            }
        }).start();
    }

    void decrypt(final EncryptedFile encryptedFile, final Runnable onFinish) {
        new AsyncTask<EncryptedFile, Void, File>() {
            @Override
            protected File doInBackground(EncryptedFile... encryptedFiles) {
                return getFile(encryptedFile, onFinish);
            }

            @Override
            protected void onPostExecute(File tempFile){
                if (tempFile != null) {
                    if (tempFile.getParentFile().equals(Storage.getTempFolder())) {
                        tempFile = new File(Storage.getTempFolder(), tempFile.getName());
                    }
                    Uri uri = OurFileProvider.getUriForFile(context, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile);
                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    String mimeType = myMime.getMimeTypeFromExtension(encryptedFile.getType());
                    newIntent.setDataAndType(uri, mimeType);
                    newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //altIntent: resort to using file provider when content provider does not work.
                    Intent altIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    Uri rawuri = Uri.fromFile(tempFile);
                    altIntent.setDataAndType(rawuri, mimeType);
                    afterDecrypt(newIntent, altIntent);
                }
            }
        }.execute(encryptedFile);
    }

    void sendMultiple(final ArrayList<FilesListFragment.DecryptArgHolder> args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Uri> uris = new ArrayList<Uri>();
                Set<String> mimes = new HashSet<String>();
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                for (FilesListFragment.DecryptArgHolder arg : args) {
                    File tempFile = getFile(arg.encryptedFile, arg.onFinish);
                    //File specified is not invalid
                    if (tempFile != null) {
                        if (tempFile.getParentFile().equals(Storage.getTempFolder()))
                            tempFile = new java.io.File(Storage.getTempFolder(), tempFile.getName());
                        uris.add(OurFileProvider.getUriForFile(context, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile));
                        mimes.add(myMime.getMimeTypeFromExtension(arg.encryptedFile.getType()));

                    }
                }
                if (uris.size() == 0 || mimes.size() == 0)
                    return;
                Intent newIntent;
                if (uris.size() == 1) {
                    newIntent = new Intent(Intent.ACTION_SEND);
                    newIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                } else {
                    newIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    newIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                }
                if (mimes.size() > 1)
                    newIntent.setType("text/plain");                        //Mixed filetypes
                else
                    newIntent.setType(new ArrayList<String>(mimes).get(0));
                newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooserIntent = generateCustomChooserIntent(newIntent, uris);
                try {
                    startActivity(Intent.createChooser(chooserIntent, CustomApp.context.getString(R.string.Dialog__send_file)));
                    FilesActivity.onPauseDecision.startActivity();
                } catch (android.content.ActivityNotFoundException e) {
                    Util.toast(context, CustomApp.context.getString(R.string.Error__no_activity_view), Toast.LENGTH_LONG);
                    FilesActivity.onPauseDecision.finishActivity();
                }
            }
        }).start();
    }

    File getFile(final EncryptedFile encryptedFile, final Runnable onfinish) {
        CryptStateListener listener = new CryptStateListener() {
            @Override
            public void updateProgress(int progress) {
                updatePBar(encryptedFile, progress);
            }

            @Override
            public void setMax(int max) {
                maxPBar(encryptedFile, max);
            }

            @Override
            public void onFailed(int statCode) {
                String message;
                switch (statCode) {
                    case Config.wrong_password:
                        message = getString(R.string.Error__wrong_password);
                        break;
                    case Config.file_not_found:
                        message = getString(R.string.Error__file_not_found);
                        break;
                    default:
                        message = getString(R.string.Error__unknown);
                }
                alert(message);
            }

            @Override
            public void Finished() {

                onfinish.run();
            }
        };
        return encryptedFile.readFile(listener);
    }

    void afterDecrypt(Intent newIntent, Intent altIntent) {
        try {
            startActivity(newIntent);
            FilesActivity.onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            try {
                startActivity(altIntent);
                FilesActivity.onPauseDecision.startActivity();
            } catch (android.content.ActivityNotFoundException e2) {
                Util.toast(context, getString(R.string.Error__no_activity_view), Toast.LENGTH_LONG);
                FilesActivity.onPauseDecision.finishActivity();
            }
        } catch (IllegalStateException e) {
            //duh why you leave so early
            FilesActivity.onPauseDecision.finishActivity();
        }
    }

    void alert(String message) {
        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        };
        Util.alert(context, getString(R.string.Error__decrypt_file), message, click, null);
    }

    void updatePBar(SecrecyFile file, int progress) {
        if (file.getProgressBar() != null)
            file.getProgressBar().setProgress(progress);
    }

    void maxPBar(EncryptedFile file, int max) {
        if (file.getProgressBar() != null)
            file.getProgressBar().setMax(max);
    }

    private Intent generateCustomChooserIntent(Intent prototype, ArrayList<Uri> uris) {
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
        Intent chooserIntent;

        Intent dummy = new Intent(prototype.getAction());
        dummy.setType(prototype.getType());
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(dummy, 0);

        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName.equalsIgnoreCase("com.doplgangr.secrecy"))
                    continue;

                HashMap<String, String> info = new HashMap<String, String>();
                info.put("packageName", resolveInfo.activityInfo.packageName);
                info.put("className", resolveInfo.activityInfo.name);
                info.put("simpleName", String.valueOf(resolveInfo.activityInfo.loadLabel(context.getPackageManager())));
                intentMetaInfo.add(info);
                for (Uri uri : uris)
                    context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (!intentMetaInfo.isEmpty()) {
                // sorting for nice readability
                Collections.sort(intentMetaInfo, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> map, HashMap<String, String> map2) {
                        return map.get("simpleName").compareTo(map2.get("simpleName"));
                    }
                });

                // create the custom intent list
                for (HashMap<String, String> metaInfo : intentMetaInfo) {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(metaInfo.get("packageName"));
                    targetedShareIntent.setClassName(metaInfo.get("packageName"), metaInfo.get("className"));
                    targetedShareIntents.add(targetedShareIntent);
                }
                chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), CustomApp.context.getString(R.string.Dialog__send_file));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[targetedShareIntents.size()]));
                return chooserIntent;
            }
        }

        return new Intent(Intent.ACTION_SEND);  //Unable to do anything. Duh.
    }

    void finish() {
        getActivity().finish();
    }
}
