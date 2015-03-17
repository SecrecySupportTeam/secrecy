package com.doplgangr.secrecy.jobs;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.doplgangr.secrecy.events.ThumbLoadDoneEvent;
import com.doplgangr.secrecy.exceptions.SecrecyFileException;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.utils.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

public class ThumbnailLoadJob extends Job {
    public static final int PRIORITY = 10;
    private final ImageView imageView;
    private final int avatar_size;
    private final EncryptedFile encryptedFile;

    public ThumbnailLoadJob(EncryptedFile encryptedFile, int size, ImageView imageView) {
        super(new Params(PRIORITY));
        this.encryptedFile = encryptedFile;
        this.avatar_size = size;
        this.imageView = imageView;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        try {
            Bitmap bm = encryptedFile.getEncryptedThumbnail().getThumb(avatar_size);
            EventBus.getDefault().post(new ThumbLoadDoneEvent(encryptedFile, imageView, bm));
        } catch(SecrecyFileException e){
            Util.log("No bitmap available!");
        }
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        throwable.printStackTrace();
        return false;
    }
}