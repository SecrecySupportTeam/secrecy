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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import java.io.File;
import java.util.ArrayList;

public class FileObserver extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_FOREGROUND = 0;
    private final ArrayList<MyFileObserver> fileObs = new ArrayList<MyFileObserver>();
    private NotificationManager mNotificationManager;

    public FileObserver() {
        super("File Handler");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("FileOb", "onStart " + intent.getStringExtra(Config.file_extra));
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        File file = new java.io.File(intent.getStringExtra(Config.file_extra));
        MyFileObserver fileOb = new MyFileObserver(file.getParent());
        fileOb.setup(file);
        fileOb.startWatching();
        fileObs.add(fileOb);
        sendNotif(String.format(getString(R.string.files_decrypted_notif), fileObs.size()), true);
        return Service.START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("FileOb", "onHandleIntent");

    }

    private void sendNotif(String msg, Boolean ongoing) {
        Intent intent = new Intent(this, FileObserver.class);
        intent.putExtra(Config.file_extra, msg);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setOngoing(ongoing)
                .setContentText(msg);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(NOTIFICATION_FOREGROUND, mBuilder.build());
        if (ongoing)
            startForeground(NOTIFICATION_FOREGROUND, mBuilder.build());
        else
            stopForeground(true);
        Log.d("FileOb", "Notification sent successfully.");
    }

    @Override
    public void onDestroy() {
        Log.d("FileOb", "IntentService Ondestroy.");
        for (MyFileObserver fileOb : fileObs) {
            fileOb.file.delete();
            fileOb.stopWatching();
        }
        mNotificationManager.cancelAll();
        super.onDestroy();
    }

    class MyFileObserver extends android.os.FileObserver {
        public final String absolutePath;
        public java.io.File file;
        long timestamp;

        public MyFileObserver(String path) {
            super(path, android.os.FileObserver.ALL_EVENTS);
            absolutePath = path;
        }

        public void setup(java.io.File file) {
            timestamp = file.lastModified();
            this.file = file;
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
            if ((android.os.FileObserver.ACCESS & event) != 0) {
                Util.log(absolutePath + "/" + path + " is accessed/read");
                stopWatching();
                kill();
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

        void kill() {
            for (int i = 0; i < 10; i++) {
                Util.log(
                        "Working... " + (i + 1) + "/5 @ "
                                + SystemClock.elapsedRealtime()
                );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            Util.log("Delete File @ " + SystemClock.elapsedRealtime());
            file.delete();
            fileObs.remove(this);
            if (fileObs.size() == 0)
                sendNotif(getString(R.string.all_temp_deleted), false);
            else
                sendNotif(String.format(getString(R.string.files_decrypted_notif), fileObs.size()), true);
        }

    }
}
