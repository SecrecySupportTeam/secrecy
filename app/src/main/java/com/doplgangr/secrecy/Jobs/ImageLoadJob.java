package com.doplgangr.secrecy.Jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ProgressBar;

import com.doplgangr.secrecy.Events.ImageLoadDoneEvent;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.io.IOUtils;

import javax.crypto.CipherInputStream;

import de.greenrobot.event.EventBus;
import uk.co.senab.photoview.PhotoView;

public class ImageLoadJob extends Job {
    public static final int PRIORITY = 10;
    private final PhotoView imageView;
    private final EncryptedFile encryptedFile;
    private final ProgressBar pBar;
    private final Integer mNum;

    public ImageLoadJob(Integer mNum, EncryptedFile encryptedFile, PhotoView imageView, ProgressBar pBar) {
        super(new Params(PRIORITY));
        this.mNum = mNum;
        this.encryptedFile = encryptedFile;
        this.imageView = imageView;
        this.pBar = pBar;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        CipherInputStream imageStream =
                encryptedFile.readStream(new CryptStateListener() {
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
}