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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Util {
    public static final DialogInterface.OnClickListener emptyClickListener = new DialogInterface.OnClickListener() {
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

    public static void toast(final Activity context, final String msg, final Integer duration) {
        context.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, msg, duration).show();
            }
        });
    }

    public static void log(Object object) {
        Log.d("Secrecy", object + ""
        );
    }

    public static Map<String, java.io.File> getAllStorageLocations() {
        Map<String, java.io.File> map = new TreeMap<String, File>();

        List<String> mMounts = new ArrayList<String>(99);
        //List<String> mVold = new ArrayList<String>(99);
        mMounts.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        try {
            java.io.File mountFile = new java.io.File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    //if (line.startsWith("/dev/block/vold/")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[1];
                    mMounts.add(element);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         try {
         java.io.File voldFile = new java.io.File("/system/etc/vold.fstab");
         if (voldFile.exists()) {
         Scanner scanner = new Scanner(voldFile);
         while (scanner.hasNext()) {
         String line = scanner.nextLine();
         //if (line.startsWith("dev_mount")) {
         String[] lineElements = line.split(" ");
         String element = lineElements[2];

         if (element.contains(":"))
         element = element.substring(0, element.indexOf(":"));
         mVold.add(element);
         }
         }
         } catch (Exception e) {
         e.printStackTrace();
         }
         **/

        /*
        for (int i = 0; i < mMounts.size(); i++) {
            String mount = mMounts.get(i);
            if (!mVold.contains(mount))
                mMounts.remove(i--);
        }
        mVold.clear();
        */

        List<String> mountHash = new ArrayList<String>(99);

        for (String mount : mMounts) {
            java.io.File root = new java.io.File(mount);
            Log.d(mount, "is checked");
            Log.d(mount, root.exists() + " " + root.isDirectory() + " " + canWrite(root));
            if (canWrite(root)) {
                Log.d(mount, "is writable");
                java.io.File[] list = root.listFiles();
                String hash = "[";
                if (list != null)
                    for (java.io.File f : list)
                        hash += f.getName().hashCode() + ":" + f.length() + ", ";
                hash += "]";
                if (!mountHash.contains(hash)) {
                    String key = root.getAbsolutePath() + " (" + org.apache.commons.io
                            .FileUtils.byteCountToDisplaySize(
                                    root.getUsableSpace()
                            ) + " free space)";
                    mountHash.add(hash);
                    map.put(key, root);
                }
            }
        }

        mMounts.clear();
        return map;
    }

    public static Boolean canWrite(java.io.File root) {
        if (!root.exists())
            return false;
        if (!root.isDirectory())
            return false;
        try {
            java.io.File file = File.createTempFile("TEMP", null, root);
            return file.delete();
        } catch (Exception e) {
            //Failed to create files
            return false;
        }

    }
}
