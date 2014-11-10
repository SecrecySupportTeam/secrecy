package com.doplgangr.secrecy.Jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.widget.ProgressBar;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.Events.ImageLoadDoneEvent;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Encryption.SecrecyCipherInputStream;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;
import uk.co.senab.photoview.PhotoView;

public class ImageLoadJob extends Job {
    public static final int PRIORITY = 10;
    private final PhotoView imageView;
    private final EncryptedFile encryptedFile;
    private final ProgressBar pBar;
    private final Integer mNum;
    private boolean isObsolet = false;
    private BitmapFactory.Options options;

    public ImageLoadJob(Integer mNum, EncryptedFile encryptedFile, PhotoView imageView,
                        ProgressBar pBar) {
        super(new Params(PRIORITY));
        this.mNum = mNum;
        this.encryptedFile = encryptedFile;
        this.imageView = imageView;
        this.pBar = pBar;
        options = new BitmapFactory.Options();
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options) {
        int pixel = options.outHeight * options.outWidth;
        int inSampleSize = 1;

        Util.log("Image input size:" + options.outHeight, options.outWidth,
                "(", (pixel / 1024 / 1024), ") megapixel");
        while ((pixel / inSampleSize) > Config.MAX_PIXEL) {
            inSampleSize *= 2;
        }
        Util.log("Image scaled to:", ( pixel / 1024 / 1024 / inSampleSize), "megapixel");
        return inSampleSize;
    }

    public void setObsolet(boolean isObsolet) {
        this.isObsolet = isObsolet;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (isObsolet) {
            return;
        }
        SecrecyCipherInputStream imageStream =
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
            try {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(imageStream, null, options);
                options.inSampleSize = calculateInSampleSize(options);
                options.inJustDecodeBounds = false;

                imageStream =
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

                if (!isObsolet) {
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream, null, options);
                    EventBus.getDefault().post(new ImageLoadDoneEvent(mNum, imageView, bitmap, pBar));

                } else {
                    EventBus.getDefault().post(new ImageLoadDoneEvent(null, null, null, null));
                }
            } catch (OutOfMemoryError e) {
                EventBus.getDefault().post(new ImageLoadDoneEvent(null, null, null, null));
            }
        }
    }

    @Override
    protected void onCancel() {
        options.requestCancelDecode();
        isObsolet = true;
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        throwable.printStackTrace();
        return false;
    }
}