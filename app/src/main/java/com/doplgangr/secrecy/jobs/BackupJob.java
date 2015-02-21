package com.doplgangr.secrecy.jobs;

import android.content.Context;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.events.BackUpDoneEvent;
import com.doplgangr.secrecy.events.BackingUpFileEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.greenrobot.event.EventBus;

/**
 * Created by matthew on 11/2/14.
 */
public class BackupJob extends Job {
    private static final int PRIORITY = 10;   //High. Same as UI jobs
    private static final int RETRY_LIMIT = 5; //Shouldn't try too much.
    private final File backupPath;    //Path of the folder to backup
    private final File backupFile; //Path of backupFile
    private final Context context;

    public BackupJob(Context context, File backupPath, File backupFile) {
        super(new Params(PRIORITY)
                .groupBy(backupPath.getAbsolutePath()));
        this.context = context;
        this.backupPath = backupPath;
        this.backupFile = backupFile;
        if (!backupPath.isDirectory())
            throw new IllegalArgumentException("Backup Path must be a directory");
        if (backupFile.isDirectory())
            throw new IllegalArgumentException("Backup File must be a file");
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        FileOutputStream fos = new FileOutputStream(backupFile);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos, Config.BLOCK_SIZE));
        byte[] buffer = new byte[Config.BUFFER_SIZE];

        Iterator it = FileUtils.iterateFiles(backupPath, null, true);
        while (it.hasNext()) {
            File fileToBackup = ((File) it.next());
            EventBus.getDefault().post(new BackingUpFileEvent(backupPath.getAbsolutePath(), fileToBackup.getName()));
            ZipEntry newEntry = new ZipEntry(fileToBackup.getAbsolutePath());
            zos.putNextEntry(newEntry);

            BufferedInputStream in =
                    new BufferedInputStream(new FileInputStream(fileToBackup), Config.BLOCK_SIZE);

            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            in.close();

        }
        zos.closeEntry();
        zos.close();

        EventBus.getDefault().post(new BackUpDoneEvent(backupPath, backupFile));

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
