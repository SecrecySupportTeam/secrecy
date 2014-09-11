package com.doplgangr.secrecy.FileSystem;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.MainActivity_;

import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by matthew on 8/22/14.
 */
@EIntentService
public class FileOptionsService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_FOREGROUND = 0;
    OutputStream os = null;
    private NotificationManager mNotificationManager;
    private int count = 0;
    private int countAddFiles = 0;

    public FileOptionsService() {
        super(FileOptionsService.class.getName());
    }

    @ServiceAction
    void delete(final java.io.File file) {
        count++;
        sendNotif("", true);
        class StaticTask extends AsyncTask<Void, Void, OutputStream> {

            @Override
            protected OutputStream doInBackground(Void... voids) {
                try {
                    os = new FileOutputStream(file);
                } catch (Exception ignored) {

                }
                return os;
            }

            @Override
            public void onPostExecute(OutputStream os) {
                long size = 0;
                if (file != null)
                    size = file.length();
                try {
                    FileUtils.forceDelete(file);
                } catch (Exception ignored) {
                } finally {
                    storage.shredFile(os, size);
                    count--;
                    if (count == 0)
                        sendNotif("", false);
                }
            }

        }

        new StaticTask().execute();
    }

    @ServiceAction
    void addFile(Vault secret, final Intent data) {
        final Context context = getApplicationContext();
        String filename = secret.addFile(context, data.getData());
        Uri thumbnail = storage.saveThumbnail(context, data.getData(), filename);
        if (thumbnail != null) {
            secret.addFile(context, thumbnail);
            delete(new java.io.File(thumbnail.getPath()));
        }
        delete(new java.io.File(data.getData().getPath())); //Try to delete original file.
        try {
            context.getContentResolver().delete(data.getData(), null, null); //Try to delete under content resolver
        } catch (Exception ignored) {
            //ignore cannot delete original file
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //ignored
    }

    private void sendNotif(String msg, Boolean ongoing) {
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity_.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                NOTIFICATION_ID, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(getString(R.string.App__name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setOngoing(ongoing)
                .setContentIntent(contentIntent)
                .setContentText(msg);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(NOTIFICATION_FOREGROUND, mBuilder.build());
        if (ongoing)
            startForeground(NOTIFICATION_FOREGROUND, mBuilder.build());
        else
            stopForeground(true);
        Util.log("FileOb", "Notification sent successfully.");
    }

}
