package com.doplgangr.secrecy.FileSystem;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.MainActivity_;

import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;

/**
 * Created by matthew on 8/22/14.
 */
@EIntentService
public class FileOptionsService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_FOREGROUND = 0;
    private NotificationManager mNotificationManager;
    private int count = 0;

    public FileOptionsService() {
        super(FileOptionsService.class.getName());
    }

    @ServiceAction
    void delete(java.io.File file) {
        count++;
        sendNotif("Shredding Files...", true);
        storage.shredFile(file);
        count--;
        if (count == 0)
            sendNotif("Done.", false);
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
                .setContentTitle(getString(R.string.app_name))
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
