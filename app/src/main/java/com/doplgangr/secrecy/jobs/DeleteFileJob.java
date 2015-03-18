package com.doplgangr.secrecy.jobs;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DeleteFileJob extends Job {
    private static final int PRIORITY = 2;   //Slightly higher than shredding files
    private final Context context = CustomApp.context;
    private final File file;
    private OutputStream os = null;
    private long size = 0;
    private Uri uri;

    public DeleteFileJob(File file) {
        super(new Params(PRIORITY)
                .groupBy(file.getAbsolutePath()));   //group according to file name
        this.file = file;
    }

    public void addURI(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        os = new FileOutputStream(file);
        if (file != null)
            size = file.length();
        Util.log("Delete ", file);
        file.delete();
        FileUtils.forceDelete(file);
        if (uri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                DocumentsContract.deleteDocument(context.getContentResolver(), uri); //For kitkat users
            context.getContentResolver().delete(uri, null, null); //Try to delete under content resolver
        }
        new SingleMediaScanner(context, file); //Rescan and remove from gallery
        Storage.shredFile(os, size, file);
        file.delete();
        FileUtils.forceDelete(file);
    }

    @Override
    protected void onCancel() {
        //Rarhhh go die.
        new SingleMediaScanner(context, file); //Rescan and remove from gallery
        Storage.shredFile(os, size, file);
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        throwable.printStackTrace();
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;  //This is quite reasonable. Isn't it?
    }

    public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

        private final MediaScannerConnection mMs;
        private final java.io.File mFile;

        public SingleMediaScanner(Context context, java.io.File f) {
            mFile = f;
            mMs = new MediaScannerConnection(context, this);
            mMs.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mMs.scanFile(mFile.getAbsolutePath(), null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mMs.disconnect();
        }

    }
}