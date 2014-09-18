package com.doplgangr.secrecy.Jobs;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.doplgangr.secrecy.FileSystem.File;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

public class ThumbnailLoadJob extends Job {
    public static final int PRIORITY = 10;
    private final ImageView imageView;
    private final int avatar_size;
    private final File file;

    public ThumbnailLoadJob(File file, int size, ImageView imageView) {
        super(new Params(PRIORITY));
        this.file = file;
        this.avatar_size = size;
        this.imageView = imageView;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Bitmap bm = file.getThumb(avatar_size);
        EventBus.getDefault().post(new ThumbLoadDoneEvent(file, imageView, bm));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        throwable.printStackTrace();
        return false;
    }

    public class ThumbLoadDoneEvent {
        public File file;
        public ImageView imageView;
        public Bitmap bitmap;

        public ThumbLoadDoneEvent(File file, ImageView imageView, Bitmap bitmap) {
            this.file = file;
            this.imageView = imageView;
            this.bitmap = bitmap;
        }
    }

}