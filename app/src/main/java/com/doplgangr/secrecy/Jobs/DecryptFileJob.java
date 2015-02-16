package com.doplgangr.secrecy.Jobs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.doplgangr.secrecy.Events.OpenFileWithIntentEvent;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.FileSystem.OurFileProvider;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.File;

import de.greenrobot.event.EventBus;

public class DecryptFileJob extends Job {
    private static final int PRIORITY = 9;   //High. Lower than UI jobs
    private final EncryptedFile encryptedFile;
    private final CryptStateListener onFinish;
    private final Activity context;

    public DecryptFileJob(Activity context, EncryptedFile encryptedFile, CryptStateListener onFinish) {
        super(new Params(PRIORITY));
        this.encryptedFile = encryptedFile;
        this.onFinish = onFinish;
        this.context = context;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        File tempFile = encryptedFile.readFile(onFinish);

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
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            //altIntent: resort to using file provider when content provider does not work.
            Intent altIntent = new Intent(android.content.Intent.ACTION_VIEW);
            Uri rawuri = Uri.fromFile(tempFile);
            altIntent.setDataAndType(rawuri, mimeType);
            EventBus.getDefault().post(new OpenFileWithIntentEvent(newIntent, altIntent));
        }
    }


    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        throwable.printStackTrace();
        return false;   //Should Not Rerun
    }
}