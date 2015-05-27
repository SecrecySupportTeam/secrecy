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

package com.doplgangr.secrecy.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.R;

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
                    a.setNegativeButton(context.getString(R.string.CANCEL), negative);
                a.setCancelable(false);
                a.show();
            }

        });
    }

    public static void alert(final Context context,
                             final String title, final String message,
                             final DialogInterface.OnClickListener ok) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {


            @Override
            public void run() {
                AlertDialog.Builder a = new AlertDialog.Builder(context);
                if (title != null)
                    a.setTitle(title);
                if (message != null)
                    a.setMessage(message);
                if (ok != null)
                    a.setPositiveButton(context.getString(R.string.OK), ok);
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

    public static void log(Object... objects) {
        String log = "";
        for (Object object : objects)
            log += " " + object;
        Log.d("SecrecyLogs", log);
    }

    public static Map<String, File> getAllStorageLocations() {
        Map<String, File> map = new TreeMap<String, File>();

        List<String> mMounts = new ArrayList<String>(99);
        //List<String> mVold = new ArrayList<String>(99);
        mMounts.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        try {
            File mountFile = new File("/proc/mounts");
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

        List<String> mountHash = new ArrayList<String>(99);

        for (String mount : mMounts) {
            File root = new File(mount);
            Util.log(mount, "is checked");
            Util.log(mount, root.exists(), root.isDirectory(), canWrite(root));
            if (canWrite(root)) {
                Util.log(mount, "is writable");
                File[] list = root.listFiles();
                String hash = "[";
                if (list != null)
                    for (File f : list)
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

    public static Boolean canWrite(File root) {
        if (root == null || !root.exists() || !root.isDirectory()) {
            return false;
        }
        try {
            File file = File.createTempFile("TEMP", null, root);
            return file.delete();
        } catch (Exception e) {
            //Failed to create files
            return false;
        }
    }

    public static void openURI(String uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(uri));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CustomApp.context.startActivity(i);

    }

    public static String getFileTypeFromExtension(String extension) {
        if (extension.length() > 0) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return "application/octet-stream";
    }

    public static void loadSelectedImageSize(String imageSize){
        switch (imageSize) {
            case "0":
                Util.log("Setting image size to: " + Config.IMAGE_SIZE_SMALL);
                Config.selectedImageSize = Config.IMAGE_SIZE_SMALL;
                break;
            case "1":
                Util.log("Setting image size to: " + Config.IMAGE_SIZE_MEDIUM);
                Config.selectedImageSize = Config.IMAGE_SIZE_MEDIUM;
                break;
            case "2":
                Util.log("Setting image size to: " + Config.IMAGE_SIZE_LARGE);
                Config.selectedImageSize = Config.IMAGE_SIZE_LARGE;
                break;
        }
    }
}
