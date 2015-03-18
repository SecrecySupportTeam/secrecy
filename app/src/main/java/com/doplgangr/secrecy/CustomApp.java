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

import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.NetworkUtilImpl;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;


import java.security.Security;

import de.greenrobot.event.EventBus;

public class CustomApp extends Application {
    public static Context context;
    public static String VERSIONNAME = "";
    public static JobManager jobManager;

    static{
        Security.insertProviderAt(
                new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

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
        Storage.deleteTemp(); //Start clean every time!!
        VaultHolder.getInstance().clear();


        Configuration configuration = new Configuration.Builder(this)
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(1)//up to 1 consumers at a time
                .loadFactor(1)//1 jobs per consumer
                .networkUtil(new NetworkUtilImpl(this))
                .build();

        jobManager = new JobManager(this, configuration);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        EventBus.getDefault().post(new LowMemoryEvent());
    }

    private class LowMemoryEvent {
    }

}
