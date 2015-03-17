package com.doplgangr.secrecy.jobs;

import android.content.Context;
import android.net.Uri;

import com.doplgangr.secrecy.events.AddingFileDoneEvent;
import com.doplgangr.secrecy.events.AddingFileEvent;
import com.doplgangr.secrecy.events.NewFileEvent;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.ipaulpro.afilechooser.utils.FileUtils.getPath;

public class AddFileJob extends Job {
    private static final int PRIORITY = 9;   //High. Lower than UI jobs
    private static final int RETRY_LIMIT = 5; //Shouldn't try too much.
    private final Vault vault;
    private final Uri uri;
    private final Context context;

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
        EventBus.getDefault().post(new AddingFileEvent(vault, uri.toString()));
        Util.log("Adding file: ", uri);
        EncryptedFile returnedEncryptedFile = vault.addFile(context, uri);
        EventBus.getDefault().post(new NewFileEvent(returnedEncryptedFile));

        EventBus.getDefault().post(new AddingFileDoneEvent(vault));
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