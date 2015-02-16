package com.doplgangr.secrecy.Jobs;

import android.app.Activity;
import android.os.Environment;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.File;

public class RestoreFileJob extends Job {
    private static final int PRIORITY = 9;   //High. Lower than UI jobs
    private final EncryptedFile encryptedFile;
    private final CryptStateListener onFinish;
    private final Activity context;

    public RestoreFileJob(Activity context, EncryptedFile encryptedFile, CryptStateListener onFinish) {
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

        File storedFile = new File(Environment.getExternalStorageDirectory(), encryptedFile.getDecryptedFileName());
        if (tempFile == null) {
            Util.alert(context,
                    CustomApp.context.getString(R.string.Error__decrypting_file),
                    CustomApp.context.getString(R.string.Error__decrypting_file_message),
                    Util.emptyClickListener,
                    null
            );
            return;
        }
        tempFile.renameTo(storedFile);
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