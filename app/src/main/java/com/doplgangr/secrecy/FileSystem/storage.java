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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Util;

import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class storage {

    public static void DeleteRecursive(java.io.File directory) {
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    public static java.io.File getTempFolder() {
        java.io.File tempDir = CustomApp.context.getExternalCacheDir();
        if (tempDir == null)                                                // when all else fails
            tempDir = CustomApp.context.getFilesDir();
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (IOException e) {
            Util.log(e);
        }
        return tempDir;
    }

    private static String getAbsTempFolder() {
        return getTempFolder().getAbsolutePath() + "/";
    }

    private static String ROOT() {
        SharedPreferences settings = CustomApp.context.getSharedPreferences(Config.settingsStore, 0);
        return settings.getString(Config.root, Environment.getExternalStorageDirectory()
                .getAbsoluteFile()
                + "/SECRECYFILES");
    }

    public static java.io.File getRoot() {
        java.io.File tempDir = new java.io.File(ROOT());
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (IOException e) {
            Util.log(e);
        }
        return tempDir;
    }

    public static Boolean setRoot(String root) {
        SharedPreferences settings = CustomApp.context.getSharedPreferences(Config.settingsStore, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Config.root, root);
        return editor.commit();
    }

    public static void deleteTemp() {
        DeleteRecursive(getTempFolder());                           //Delete temp files generated
        DeleteRecursive(CustomApp.context.getCacheDir());           //Delete App cache
        DeleteRecursive(CustomApp.context.getExternalCacheDir());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            for (java.io.File externalCacheDir : CustomApp.context.getExternalCacheDirs())
                DeleteRecursive(externalCacheDir);                      //Just to be sure
    }

    public static Uri saveThumbnail(Context context, Uri uri, String filename) {
        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(uri);
            java.io.File thumbpath = new java.io.File(getAbsTempFolder() + "/" + "_thumb" + filename);
            if (thumbpath.exists())
                thumbpath.delete();
            thumbpath.createNewFile();
            FileOutputStream out = new FileOutputStream(thumbpath);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (options.outWidth != -1 && options.outHeight != -1) {
                bitmap = ThumbnailUtils.extractThumbnail(bitmap
                        , 150, 150);
            } else {
                bitmap = ThumbnailUtils.createVideoThumbnail(
                        getRealPathFromURI(context, uri), MediaStore.Video.Thumbnails.MICRO_KIND);
            }
            if (bitmap == null) {
                out.close();
                thumbpath.delete();
                return null;
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            return Uri.fromFile(thumbpath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    public static Bitmap getThumbnail(java.io.File file) {
        if (file != null)
            if (file.exists())
                if (file.length() > 0)
                    return BitmapFactory.decodeFile(file.getAbsolutePath());
        return null;
    }

    private static String getRealPathFromURI(Context context, final Uri contentURI) {
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (idx == -1) {
                return contentURI.getPath();
            }
            String rvalue = cursor.getString(idx);
            cursor.close();
            return rvalue;
        }
    }
}
