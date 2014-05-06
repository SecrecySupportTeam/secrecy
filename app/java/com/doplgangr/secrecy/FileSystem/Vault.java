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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.Util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.CipherOutputStream;

/**
 * Created by Matthew on 3/22/2014.
 */
public class Vault {
    public final ArrayList<File> files = new ArrayList<File>();
    private final String name;
    private final String path;
    public Boolean wrongPass = true;
    private String key = "0123784";

    public Vault(String name, String secret) {
        this.key = secret;
        this.name = name;
        path = storage.getRoot().getAbsolutePath() + "/" + name;
        initialize();
    }

    public Vault(String name, String secret, Boolean temp) {
        this.key = secret;
        this.name = name;
        path = storage.getRoot().getAbsolutePath() + "/" + name;
    }

    public String getName() {
        return name;
    }

    public void initialize() {
        java.io.File folder = new java.io.File(path);
        String regex = "^((?!_thumb|.nomedia).)*$";
        final Pattern p = Pattern.compile(regex);
        List<java.io.File> absFiles = Arrays.asList(
                folder.listFiles(
                        new FileFilter() {
                            @Override
                            public boolean accept(java.io.File file) {
                                p.matcher(file.getName()).matches();
                                Util.log(p.matcher(file.getName()).matches());
                                return p.matcher(file.getName()).matches();
                            }
                        }
                )
        );
        java.io.File nomedia = new java.io.File(storage.getRoot().getAbsolutePath() + "/" +
                name + "/.nomedia");
        if (!nomedia.exists())
            return;
        File nomediafile = new File(nomedia, key);
        java.io.File tempnomedia = nomediafile.readFile(new CryptStateListener() {
            @Override
            public void updateProgress(int progress) {
            }

            @Override
            public void setMax(int max) {
            }

            @Override
            public void onFailed(int statCode) {
            }

            @Override
            public void Finished() {

            }
        });
        if (tempnomedia != null) {
            wrongPass = false;
            tempnomedia.delete();
        }
        Log.d("Password is Wrong=", wrongPass + "");

        files.clear();
        for (java.io.File absfile : absFiles)
            files.add(new File(absfile, key));
    }

    public String addFile(final Context context, final Uri uri) {
        String filename = uri.getLastPathSegment();
        if (!filename.contains("_thumb")) {
            ContentResolver cR = context.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(uri));
            if (type != null)
                filename = FilenameUtils.removeExtension(filename) + "." + type;
        }
        InputStream is = null;
        OutputStream out = null;
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            java.io.File addedFile = new java.io.File(path + "/" + filename);
            addedFile.delete();
            addedFile.createNewFile();
            is = new BufferedInputStream(stream);
            byte buffer[] = new byte[Config.bufferSize];
            int count;
            AES_Encryptor enc = new AES_Encryptor(key);
            out = new CipherOutputStream(new FileOutputStream(addedFile),
                    enc.encryptstream()
            );
            while ((count = is.read(buffer)) != -1)
                out.write(buffer, 0, count);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        initialize();
        return filename;
    }

    public Boolean delete() {
        if (!wrongPass)
            try {
                FileUtils.deleteDirectory(new java.io.File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        return !wrongPass;
    }

}
