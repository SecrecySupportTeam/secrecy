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
    private Context context = CustomApp.context;
    private File file;
    private Uri uri;
    private long size = 0;

    public DeleteFileJob(File file) {
        super(new Params(PRIORITY)
                .groupBy(file.getAbsolutePath()));   //group according to file name
        this.file = file;
        this.uri = null;
    }

    public DeleteFileJob(Uri uri) {
        super(new Params(PRIORITY)
                .groupBy(uri.getPath()));   //group according to file name
        this.uri = uri;
        this.file = null;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (file == null) {
            // Retrieve real file from URI
            try {
                file = com.ipaulpro.afilechooser.utils.FileUtils.getFile(context, uri);
            } catch (Exception ignored) {
                // Never mind if this failed.
            }
        }
        if (file != null) {
            //1. Retrieve the file's OS and wipe its content.
            OutputStream os = new FileOutputStream(file);
            if (file != null)
                size = file.length();
            Util.log("Delete ", file);
            ShredFileJob job = new ShredFileJob(os, size, file);
            CustomApp.jobManager.addJob(job);   //Don't create yet another bg thread.

            //2. File deleted once. Rescan and remove it from gallery
            new SingleMediaScanner(context, file);
        }
        if (uri != null){
            //4. Delete under content resolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                DocumentsContract.deleteDocument(context.getContentResolver(), uri); //For kitkat users
            context.getContentResolver().delete(uri, null, null);
        }
        //3. Desperately delete again, just to be sure if shred file job failed.
        if (file != null)
            FileUtils.deleteQuietly(file);
    }

    @Override
    protected void onCancel() {
        // Final retry.
        FileUtils.deleteQuietly(file);
        new SingleMediaScanner(context, file); //Rescan and remove from gallery
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        Util.log(throwable.getMessage());
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;  //This is quite reasonable. Isn't it?
    }

    // Media scanner class that detects file deletion.
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