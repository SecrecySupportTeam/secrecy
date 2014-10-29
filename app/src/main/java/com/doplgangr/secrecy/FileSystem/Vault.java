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
import android.webkit.MimeTypeMap;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.Listeners;
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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.CipherOutputStream;

import static com.ipaulpro.afilechooser.utils.FileUtils.getPath;

public class Vault implements Serializable {
    private final String name;
    private final String path;
    public Boolean wrongPass = true;
    private String key;

    public Vault(String name, String secret) {
        this.key = secret;
        this.name = name;
        path = Storage.getRoot().getAbsolutePath() + "/" + name;
        initialize();
    }

    public Vault(String name, String secret, Boolean istemp) {
        this.key = secret;
        this.name = name;
        path = Storage.getRoot().getAbsolutePath() + "/" + name;
        //do not initialize now coz this is temp
    }

    private static boolean fileFilter(java.io.File file) {
        String regex = "^((?!_thumb|.nomedia).)*$";            //Filter out .nomedia and thumbnails
        String name = file.getName();
        final Pattern p = Pattern.compile(regex);
        p.matcher(name).matches();
        return p.matcher(name).matches();

    }

    public String getName() {
        return name;
    }

    public void initialize() {
        java.io.File nomedia = new java.io.File(Storage.getRoot().getAbsolutePath() + "/" +
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
            Storage.purgeFile(tempnomedia);
        }
        Util.log("Password is Wrong=", wrongPass);
    }

    public void iterateAllFiles(onFileFoundListener listener) {
        List<java.io.File> files = getFileList();
        for (java.io.File file : files)
            listener.dothis(new File(file, key));
    }

    public int getCount() {
        return getFileList().size();
    }

    public String addFile(final Context context, final Uri uri) {
        String filename = uri.getLastPathSegment();
        try {
            String realPath = getPath(context, uri);
            Util.log(realPath, filename);
            filename = new java.io.File(realPath).getName();
            // If we can use real path, better use one.
        } catch (Exception ignored) {
            // Leave it.
        }
        if (!filename.contains("_thumb") && !filename.contains(".nomedia")) {
            ContentResolver cR = context.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(uri));
            if (type != null)
                filename = Base64Coder.encodeString(FilenameUtils.removeExtension(filename)) + "." +
                        type;
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

        return filename;
    }

    public File getFileInstance(String name) {
        java.io.File requestedFile = new java.io.File(path, name);
        return new File(requestedFile, key);
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

    private List<java.io.File> getFileList() {
        java.io.File folder = new java.io.File(path);
        return Arrays.asList(
                folder.listFiles(
                        new FileFilter() {
                            @Override
                            public boolean accept(java.io.File file) {
                                return fileFilter(file);
                            }
                        }
                )
        );
    }

    public Vault rename(String name) {
        if (wrongPass)
            return null; //bye
        java.io.File folder = new java.io.File(path);
        java.io.File newFoler = new java.io.File(folder.getParent(), name);
        if (folder.getAbsolutePath().equals(newFoler.getAbsolutePath()))
            return this; //same name, bye
        try {
            FileUtils.copyDirectory(folder, newFoler);
        } catch (IOException e) {
            // New Folder should be cleared. Only preserver old folder
            try {
                FileUtils.deleteDirectory(newFoler);
            } catch (IOException ignored) {
                //ignore
            }
            return null;
        }
        try {
            FileUtils.deleteDirectory(folder);
        } catch (IOException ignored) {
            //ignored
        }
        return new Vault(name, key);
    }

    public void startWatching(final Listeners.FileObserverEventListener mListener) {
        final android.os.FileObserver observer = new android.os.FileObserver(path) { // set up a file observer to watch this directory on sd card
            @Override
            public void onEvent(int event, String filename) {
                if (filename != null) {
                    java.io.File file = new java.io.File(path, filename);
                    if (fileFilter(file)) {
                        if (event == android.os.FileObserver.CREATE || event == android.os.FileObserver.MOVED_TO)
                            mListener.add(file);
                        if (event == android.os.FileObserver.DELETE || event == android.os.FileObserver.MOVED_FROM)
                            mListener.remove(file);
                    }
                }
            }
        };
        observer.startWatching(); //START OBSERVING
    }

    public interface onFileFoundListener {
        void dothis(File file);
    }

}
