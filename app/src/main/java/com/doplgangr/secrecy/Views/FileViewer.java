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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.FileSystem.FileObserver;
import com.doplgangr.secrecy.FileSystem.FileOptionsService_;
import com.doplgangr.secrecy.FileSystem.OurFileProvider;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Listeners;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.api.BackgroundExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EFragment(R.layout.activity_file_viewer)
public class FileViewer extends Fragment {


    ActionBarActivity context;


    @AfterInject
    void onCreate() {
        context = (ActionBarActivity) getActivity();
        if (context.getSupportActionBar() != null)
            context.getSupportActionBar().setSubtitle(storage.getRoot().getAbsolutePath());
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.open_file))
                .setMessage(getString(R.string.open_file_message))
                .setView(input)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = input.getText().toString();
                        Uri file = context.getIntent().getData();
                        decrypt(new File(new java.io.File(file.getPath()), password), null, null);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        }).show();
    }


    @Background
    void addFile(Vault secret, final Intent data) {
        FileOptionsService_.intent(this)
                .addFile(secret, data)
                .start();
        onCreate();
    }


    @Background
    void decrypt(File file, final ProgressBar pBar, final Listeners.EmptyListener onFinish) {
        java.io.File tempFile = getFile(file, pBar, onFinish);
        //File specified is not invalid
        if (tempFile != null) {
            if (tempFile.getParentFile().equals(storage.getTempFolder())) {
                java.io.File newFile = new java.io.File(storage.getTempFolder(), tempFile.getName());
                tempFile = newFile;
            }
            Uri uri = OurFileProvider.getUriForFile(context, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile);
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
            String mimeType = myMime.getMimeTypeFromExtension(file.getType());
            newIntent.setDataAndType(uri, mimeType);
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //altIntent: resort to using file provider when content provider does not work.
            Intent altIntent = new Intent(android.content.Intent.ACTION_VIEW);
            Uri rawuri = Uri.fromFile(tempFile);
            altIntent.setDataAndType(rawuri, mimeType);
            afterDecrypt(newIntent, altIntent);
        }

    }

    @Background
    void sendMultiple(ArrayList<FilesListFragment.DecryptArgHolder> args) {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        Set<String> mimes = new HashSet<String>();
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        for (FilesListFragment.DecryptArgHolder arg : args) {
            java.io.File tempFile = getFile(arg.file, arg.pBar, arg.onFinish);
            //File specified is not invalid
            if (tempFile != null) {
                if (tempFile.getParentFile().equals(storage.getTempFolder()))
                    tempFile = new java.io.File(storage.getTempFolder(), tempFile.getName());
                uris.add(OurFileProvider.getUriForFile(context, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile));
                mimes.add(myMime.getMimeTypeFromExtension(arg.file.getType()));

            }
        }
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
            startActivity(Intent.createChooser(chooserIntent, getString(R.string.send_file_dialog)));
            onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            Util.toast(context, getString(R.string.error_no_activity_view), Toast.LENGTH_LONG);
            onPauseDecision.finishActivity();
        }
    }

    java.io.File getFile(final File file, final ProgressBar pBar, final Listeners.EmptyListener onfinish) {
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
        try {
            startActivity(newIntent);
            onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            try {
                startActivity(altIntent);
                onPauseDecision.startActivity();
            } catch (android.content.ActivityNotFoundException e2) {
                Util.toast(context, getString(R.string.error_no_activity_view), Toast.LENGTH_LONG);
                onPauseDecision.finishActivity();
            }
        } catch (IllegalStateException e) {
            //duh why you leave so early
            onPauseDecision.finishActivity();
        }
    }

    @UiThread
    void alert(String message) {
        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        };
        Util.alert(context, getString(R.string.error_decrypt_dialog), message, click, null);
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

    @Override
    public void onPause() {
        super.onPause();
        if (onPauseDecision.shouldFinish())
            finish();
    }

    void finish() {
        BackgroundExecutor.cancelAll(Config.cancellable_task, false);
        getActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        onPauseDecision.finishActivity();
    }

    @Override
    public void onDestroy() {
        Intent fileObserverIntent = new Intent(CustomApp.context, FileObserver.class);
        CustomApp.context.stopService(fileObserverIntent);          //Use App context since context might be null.
        BackgroundExecutor.cancelAll(Config.cancellable_task, true);
        super.onDestroy();
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
                chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), getString(R.string.send_file_dialog));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[targetedShareIntents.size()]));
                return chooserIntent;
            }
        }

        return new Intent(Intent.ACTION_SEND);  //Unable to do anything. Duh.
    }

    static class onPauseDecision {
        static Boolean pause = true;

        // An activity is started, should not pause and kill this fragment.
        static void startActivity() {
            pause = false;
        }

        // Fragment returns to top, allow it to be paused and killed.
        static void finishActivity() {
            pause = true;
        }

        static Boolean shouldFinish() {
            return pause;
        }
    }
}
