package com.doplgangr.secrecy.Jobs;

import com.doplgangr.secrecy.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class ShredFileJob extends Job {
    public static final int PRIORITY = 1;
    private OutputStream fileOs = null;
    private long size;

    public ShredFileJob(OutputStream os, long size) {
        super(new Params(PRIORITY));
        this.fileOs = os;
        this.size = size;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        Util.log("Shreddd");
        // Double check
        OutputStream os = new BufferedOutputStream(fileOs);
        try {
            for (int i = 0; i < size; i++)
                os.write(0);
        } finally {
            os.close();
        }
    }

    @Override
    protected void onCancel() {
        //Rarhhh go die.
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        Util.log("Shredding retry");
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;  //This is quite reasonable. Isn't it?
    }

}