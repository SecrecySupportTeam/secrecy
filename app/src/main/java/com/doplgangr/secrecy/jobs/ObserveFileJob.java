package com.doplgangr.secrecy.jobs;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ObserveFileJob extends Job {
    private static final int PRIORITY = 5;   //relatively high
    private final File file;
    private MyFileObserver fileOb;

    public ObserveFileJob(File file) {
        super(new Params(PRIORITY));
        this.file = file;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        if (fileOb == null) {
            fileOb = new MyFileObserver(file.getParent());
            fileOb.setup(file);
            fileOb.startWatching();
        }
    }

    @Override
    protected void onCancel() {
        if (fileOb != null)
            fileOb.kill();
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;  //This is quite reasonable. Isn't it?
    }

    class MyFileObserver extends android.os.FileObserver {
        public final String absolutePath;
        public java.io.File file;
        private ParcelFileDescriptor pfd;
        private long size;

        public MyFileObserver(String path) {
            super(path, android.os.FileObserver.ALL_EVENTS);
            absolutePath = path;
        }

        public void setup(java.io.File file) {
            this.file = file;
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY);
                size = file.length();
            } catch (IOException ignored) {
                //FINEE
            }
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null || !file.getName().equals(path)) {
                return;
            }
            if ((android.os.FileObserver.CLOSE_NOWRITE & event) != 0 ||
                    (android.os.FileObserver.CLOSE_WRITE & event) != 0) {
                Util.log(absolutePath + "/" + path + " CLOSED");
                kill();
            }
            if ((android.os.FileObserver.OPEN & event) != 0 ||
                    (android.os.FileObserver.ACCESS & event) != 0) {
                delete();
            }
        }

        void delete() {
            try {
                Thread.sleep(1000);
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException ignored2) {
                    //fine
                }
            }
        }

        void kill() {
            stopWatching();
            Util.log("Delete File @ " + SystemClock.elapsedRealtime());
            if (pfd != null) {
                Util.log(pfd.getFileDescriptor().toString());
                OutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                Storage.shredFile(fileOutputStream, size, file);
            }
        }
    }
}