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

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.FileSystem.Files.SecrecyFile;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Jobs.AddFileJob;
import com.doplgangr.secrecy.Listeners;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.api.BackgroundExecutor;

@EFragment(R.layout.activity_file_viewer)
public abstract class FileViewer extends Fragment {

    ActionBarActivity context;

    public CryptStateListener getCryptStateListener(final EncryptedFile encryptedFile, final Listeners.EmptyListener onfinish) {
        return new CryptStateListener() {
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
    }

    @UiThread
    void alert(String message) {
        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        };
        Util.alert(context, getString(R.string.Error__decrypt_file), message, click, null);
    }

    @UiThread
    void updatePBar(SecrecyFile file, int progress) {
        if (file.getProgressBar() != null)
            file.getProgressBar().setProgress(progress);
    }

    @UiThread
    void maxPBar(EncryptedFile file, int max) {
        if (file.getProgressBar() != null)
            file.getProgressBar().setMax(max);
    }

    void openWithIntent(Intent newIntent, Intent altIntent) {
        try {
            context.startActivity(newIntent);
            FilesActivity.onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            try {
                context.startActivity(altIntent);
                FilesActivity.onPauseDecision.startActivity();
            } catch (android.content.ActivityNotFoundException e2) {
                Util.toast(context,
                        CustomApp.context.getString(R.string.Error__no_activity_view),
                        Toast.LENGTH_LONG);
                FilesActivity.onPauseDecision.finishActivity();
            }
        } catch (IllegalStateException e) {
            //duh why you leave so early
            FilesActivity.onPauseDecision.finishActivity();
        }
    }

    @Override
    public void onDestroy() {
        BackgroundExecutor.cancelAll(Config.cancellable_task, true);
        super.onDestroy();
    }

    void finish() {
        getActivity().finish();
    }

    public class DecryptArgHolder {
        public final EncryptedFile encryptedFile;
        public final ProgressBar pBar;
        public final CryptStateListener onFinish;

        public DecryptArgHolder(EncryptedFile encryptedFile, ProgressBar pBar, CryptStateListener onFinish) {
            this.encryptedFile = encryptedFile;
            this.pBar = pBar;
            this.onFinish = onFinish;
        }
    }
}
