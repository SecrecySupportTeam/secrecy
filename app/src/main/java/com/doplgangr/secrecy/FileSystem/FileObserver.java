/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.FileSystem;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.MainActivity_;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class FileObserver extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_FOREGROUND = 0;
    private static final ArrayList<MyFileObserver> fileObs = new ArrayList<MyFileObserver>();
    private static int fileObsCount;
    private NotificationManager mNotificationManager;

    public FileObserver() {
        super("File Handler");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        File file = new java.io.File(intent.getStringExtra(Config.file_extra));
        MyFileObserver fileOb = new MyFileObserver(file.getParent());
        fileOb.setup(file);
        if (!fileObs.contains(fileOb)) {
            fileOb.startWatching();
            Util.log("FileOb", "onStart " + intent.getStringExtra(Config.file_extra));
            fileObs.add(fileOb);
            fileObsCount++;
        }
        sendNotif(String.format(getString(R.string.Notif__files_decrypted), fileObs.size()), true);
        return Service.START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Util.log("FileOb", "onHandleIntent");

    }

    private void sendNotif(String msg, Boolean ongoing) {
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

    @Override
    public void onDestroy() {
        Util.log("FileOb", "IntentService Ondestroy.");
        for (MyFileObserver fileOb : fileObs)
            fileOb.kill();
        mNotificationManager.cancelAll();
        super.onDestroy();
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
            //a file or directory was opened
            //if ((FileObserver.OPEN & event)!=0) {
            //    Log.d("FileOb",path + " is opened\n");
            //}
            //data was read from a file
            if ((android.os.FileObserver.CLOSE_NOWRITE & event) != 0 ||
                    (android.os.FileObserver.CLOSE_WRITE & event) != 0) {
                Util.log(absolutePath + "/" + path + " CLOSED");
                kill();
            }
            if ((android.os.FileObserver.OPEN & event) != 0 ||
                    (android.os.FileObserver.ACCESS & event) != 0) {
                delete();
            }
            //data was written to a file
            //if ((FileObserver.MODIFY & event)!=0) {
            //    Log.d("FileOb",absolutePath + "/" + path + " is modified\n");
            //}
            //someone has a file or directory open read-only, and closed it
            //if ((FileObserver.CLOSE_NOWRITE & event)!=0) {
            //    Log.d("FileOb", path + " is closed\n");
            //}
            //someone has a file or directory open for writing, and closed it
            //if ((FileObserver.CLOSE_WRITE & event)!=0) {
            //    Util.log(absolutePath + "/" + path + " is written and closed");
            //    if (file.lastModified() != timestamp) {
            //        timestamp = file.lastModified();
            //        sendNotif(path+ " is written and closed");
            //    }
            //}
            //[todo: consider combine this one with one below]
            //a file was deleted from the monitored directory
            //if ((FileObserver.DELETE & event)!=0) {
            //    //for testing copy file
// FileUtils.copyFile(absolutePath + "/" + path);
            //    Log.d("FileOb",absolutePath + "/" + path + " is deleted\n");
            //}
            //the monitored file or directory was deleted, monitoring effectively stops
            //if ((FileObserver.DELETE_SELF & event)!=0) {
            //    Log.d("FileOb",absolutePath + "/" + " is deleted\n");
            //}
            //a file or subdirectory was moved from the monitored directory
            //if ((FileObserver.MOVED_FROM & event)!=0) {
            //    Log.d("FileOb",absolutePath + "/" + path + " is moved to somewhere " + "\n");
            //}
            //a file or subdirectory was moved to the monitored directory
            //if ((FileObserver.MOVED_TO & event)!=0) {
            //    Log.d("FileOb","File is moved to " + absolutePath + "/" + path + "\n");
            //}
            //the monitored file or directory was moved; monitoring continues
            //if ((FileObserver.MOVE_SELF & event)!=0) {
            //    Log.d("FileOb",path + " is moved\n");
            //}
            //Metadata (permissions, owner, timestamp) was changed explicitly
            //if ((FileObserver.ATTRIB & event)!=0) {
            //    Log.d("FileOb",absolutePath + "/" + path + " is changed (permissions, owner, timestamp)\n");
            //}
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
                storage.shredFile(fileOutputStream, size);
            }
            fileObsCount--;
            if (fileObsCount == 0)
                sendNotif(getString(R.string.Notif__temp_deleted), false);
            else
                sendNotif(String.format(getString(R.string.Notif__files_decrypted), fileObs.size()), true);

        }
    }


}