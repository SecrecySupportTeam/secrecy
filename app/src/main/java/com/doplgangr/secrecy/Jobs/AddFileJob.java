package com.doplgangr.secrecy.Jobs;

import android.content.Context;
import android.net.Uri;

import com.doplgangr.secrecy.Events.NewFileEvent;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.ipaulpro.afilechooser.utils.FileUtils.getPath;

public class AddFileJob extends Job {
    private static final int PRIORITY = 9;   //High. Lower than UI jobs
    private static final int RETRY_LIMIT = 5; //Shouldn't try too much.
    private Vault vault;
    private Uri uri;
    private Context context;

    public AddFileJob(Context context, Vault vault, Uri uri) {
        super(new Params(PRIORITY)
                .groupBy(uri.toString()));
        this.vault = vault;
        this.uri = uri;
        this.context = context;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Util.log("Adding file: ", uri);
        EncryptedFile returnedEncryptedFile = vault.addFile(context, uri);
        EventBus.getDefault().post(new NewFileEvent(returnedEncryptedFile));
        File actualFile = new File(getPath(context, uri));
        Storage.purgeFile(actualFile, uri); //Try to delete original file.
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        throwable.printStackTrace();
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return RETRY_LIMIT;
    }
}