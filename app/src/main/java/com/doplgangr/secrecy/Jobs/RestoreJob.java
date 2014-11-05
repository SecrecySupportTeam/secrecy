package com.doplgangr.secrecy.Jobs;

import android.content.Context;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.Events.RestoreDoneEvent;
import com.doplgangr.secrecy.Events.RestoringFileEvent;
import com.doplgangr.secrecy.Exceptions.SecrecyRestoreException;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.greenrobot.event.EventBus;

/**
 * Created by matthew on 11/2/14.
 */
public class RestoreJob extends Job {
    private static final int PRIORITY = 10;   //High. Same as UI jobs
    private static final int RETRY_LIMIT = 5; //Shouldn't try too much.
    private File backupFile; //Path of backupFile
    private Context context;

    public RestoreJob(Context context, File backupFile) {
        super(new Params(PRIORITY)
                .groupBy(backupFile.getAbsolutePath()));
        this.context = context;
        this.backupFile = backupFile;
        if (backupFile.isDirectory())
            throw new IllegalArgumentException("Backup File must be a file");
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        FileInputStream fis = new FileInputStream(backupFile);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis, Config.BLOCK_SIZE));
        byte[] buffer = new byte[Config.BUFFER_SIZE];

        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            File fileToRestore = new File(ze.getName());
            EventBus.getDefault().post(new RestoringFileEvent(backupFile, fileToRestore));
            //Initialize folders
            new File(fileToRestore.getParent()).mkdirs();
            if (fileToRestore.exists())
                if (!fileToRestore.delete())
                    throw new SecrecyRestoreException("Existing File cannot be deleted.");
            if (!fileToRestore.createNewFile())
                throw new SecrecyRestoreException("New File cannot be created at" +
                        fileToRestore.getAbsolutePath() + ". Is the restore path valid?");

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToRestore), Config.BLOCK_SIZE);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }

            bos.close();
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        EventBus.getDefault().post(new RestoreDoneEvent(backupFile));

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
