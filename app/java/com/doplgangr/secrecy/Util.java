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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Matthew on 4/4/2014.
 */
public class Util {
    public static DialogInterface.OnClickListener emptyClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
        }
    };

    public static void alert(final Context context,
                             final String title, final String message,
                             final DialogInterface.OnClickListener positive,
                             final DialogInterface.OnClickListener negative) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {


            @Override
            public void run() {
                AlertDialog.Builder a = new AlertDialog.Builder(context);
                if (title != null)
                    a.setTitle(title);
                if (message != null)
                    a.setMessage(message);
                if (positive != null)
                    a.setPositiveButton(context.getString(R.string.OK), positive);
                if (negative != null)
                    a.setNegativeButton(context.getString(R.string.cancel), negative);
                a.setCancelable(false);
                a.show();
            }

        });
    }

    public static void log(Object object) {
        Log.d("Secrecy", object.toString()
        );
    }
}
