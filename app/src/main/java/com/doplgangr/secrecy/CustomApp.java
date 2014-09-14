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

package com.doplgangr.secrecy;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.doplgangr.secrecy.FileSystem.storage;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;
import com.uservoice.uservoicesdk.UserVoice;

import org.androidannotations.annotations.EApplication;

import de.greenrobot.event.EventBus;

@EApplication
public class CustomApp extends Application {
    public static Context context;
    public static String VERSIONNAME = "";
    public static JobManager jobManager;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            VERSIONNAME = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        storage.deleteTemp(); //Start clean every time!!

        jobManager = new JobManager(this);
        // Set this up once when your application launches
        UserVoice.init(Config.uservoice(this), this);  //Uservoice init
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        EventBus.getDefault().post(new LowMemoryEvent());
    }

    void generateJobManager() {
        Configuration configuration = new Configuration.Builder(this)
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Util.log(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Util.log(TAG, String.format(text, args), t);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Util.log(TAG, String.format(text, args));
                    }
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(10)//up to 10 consumers at a time
                .loadFactor(2)//2 jobs per consumer
                .build();
        jobManager = new JobManager(this, configuration);
    }

    public class LowMemoryEvent {
    }

}
