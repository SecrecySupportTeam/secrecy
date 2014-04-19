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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.doplgangr.secrecy.UpdateManager.AppVersion_;
import com.doplgangr.secrecy.UpdateManager.UpdateManager_;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity
public class MainActicity extends Activity {
    @Pref
    AppVersion_ version;
    Integer versionnow;
    String versionnow_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, ListVaultActivity_.class));
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Util.log("Cannot get package info, abort.");
            return;
        }
        if (pInfo != null) {
            versionnow = pInfo.versionCode;
            versionnow_name = pInfo.versionName;
            if (versionnow != version.no().get()) {
                Intent intent = new Intent(this, UpdateManager_.class);
                startActivity(intent);
            }
        }
        finish();
    }
}
