package com.doplgangr.secrecy.Jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ProgressBar;

import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.File;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.IOUtils;

import javax.crypto.CipherInputStream;

import de.greenrobot.event.EventBus;
import uk.co.senab.photoview.PhotoView;

public class ImageLoadJob extends Job {
    public static final int PRIORITY = 10;
    private final PhotoView imageView;
    private final File file;
    private final ProgressBar pBar;
    private final Integer mNum;

    public ImageLoadJob(Integer mNum, File file, PhotoView imageView, ProgressBar pBar) {
        super(new Params(PRIORITY));
        this.mNum = mNum;
        this.file = file;
        this.imageView = imageView;
        this.pBar = pBar;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        CipherInputStream imageStream =
                file.readStream(new CryptStateListener() {
                    @Override
                    public void updateProgress(int progress) {
                    }

                    @Override
                    public void setMax(int max) {
                    }

                    @Override
                    public void onFailed(int statCode) {
                    }

                    @Override
                    public void Finished() {
                    }
                });
        //File specified is not invalid
        if (imageStream != null) {
            //Decode image size
            byte[] bytes = IOUtils.toByteArray(imageStream);
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                bytes = null;
                EventBus.getDefault().post(new ImageLoadDoneEvent(mNum, imageView, bm, pBar));
            } catch (OutOfMemoryError e) {
                EventBus.getDefault().post(new ImageLoadDoneEvent(mNum, null, null, null));
            }
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

    public class ImageLoadDoneEvent {
        public Integer mNum;
        public PhotoView imageView;
        public Bitmap bitmap;
        public ProgressBar progressBar;

        public ImageLoadDoneEvent(Integer mNum, PhotoView imageView, Bitmap bitmap, ProgressBar progressBar) {
            this.mNum = mNum;
            this.imageView = imageView;
            this.bitmap = bitmap;
            this.progressBar = progressBar;
        }
    }

}