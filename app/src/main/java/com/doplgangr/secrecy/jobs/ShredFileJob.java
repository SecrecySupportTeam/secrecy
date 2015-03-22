package com.doplgangr.secrecy.jobs;

import com.doplgangr.secrecy.utils.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;

public class ShredFileJob extends Job {
    public static final int PRIORITY = 1;
    private OutputStream fileOs = null;
    private final long size;
    private final File file;

    public ShredFileJob(OutputStream os, long size, File file) {
        super(new Params(PRIORITY));
        this.fileOs = os;
        this.size = size;
        this.file = file;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        Util.log("Shredding, filesize",size);

        OutputStream os = new BufferedOutputStream(fileOs);
        try {
            for (int i = 0; i < size; i++)
                os.write(0);
        } finally {
            os.close();
        }
        FileUtils.forceDelete(file);
    }

    @Override
    protected void onCancel() {
        // Final try, in case it is problem associated with output stream.
        FileUtils.deleteQuietly(file);
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        Util.log(throwable.getMessage());
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;
    }

}