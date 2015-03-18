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

package com.doplgangr.secrecy.filesystem.encryption;

import android.content.Context;
import android.net.Uri;

import com.doplgangr.secrecy.exceptions.SecrecyFileException;
import com.doplgangr.secrecy.filesystem.CryptStateListener;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.filesystem.files.EncryptedFileFactory;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class Vault implements Serializable {
    private final String name;
    private final String path;
    private final String passphrase;
    private Crypter crypter;

    public Boolean wrongPass = false;

    Vault(String name, String passphrase) {
        this.name = name;
        this.passphrase = passphrase;
        path = Storage.getRoot().getAbsolutePath() + '/' + name;

        // Dont load Crypter if vault is ECB vault
        if (isEcbVault()){
            return;
        }

        try {
            crypter = new AES_CTR_Crypter(path, passphrase);
        } catch (InvalidKeyException e) {
            Util.log("Passphrase is wrong");
            wrongPass = true;
        }
    }

    Vault(String name, String passphrase, Boolean istemp) {
        this.passphrase = passphrase;
        this.name = name;
        path = Storage.getRoot().getAbsolutePath() + '/' + name;

        // Dont load Crypter if vault is ECB vault
        if (isEcbVault()){
            return;
        }

        try {
            crypter = new AES_CTR_Crypter(path, passphrase);
        } catch (InvalidKeyException e) {
            Util.log("Passphrase is wrong");
            wrongPass = true;
        }
        //do not initialize now coz this is temp
    }

    public boolean isEcbVault(){
        File nomedia = new File(path + "/.nomedia");
        File newVaultHeader = new File(path + "/.vault");

        return (nomedia.exists() && !newVaultHeader.exists());
    }

    public boolean updateFromECBVault(String passphrase) throws FileNotFoundException, InvalidKeyException, SecrecyFileException {
        @SuppressWarnings("deprecation") AES_ECB_Crypter ecb_crypter = new AES_ECB_Crypter(path, passphrase);
        Crypter newCrypter = new AES_CTR_Crypter(path, passphrase);

        List<File> files = getFileList();
        for (File file : files){
            EncryptedFile oldEncryptedFile =
                    EncryptedFileFactory.getInstance().loadEncryptedFile(file, ecb_crypter, true);
            CryptStateListener listener = new CryptStateListener() {
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
            };
            File tempFile = oldEncryptedFile.readFile(listener);
            EncryptedFileFactory.getInstance().createNewEncryptedFile(tempFile, newCrypter, this);

            File oldThumbnail = new File(path + "/_thumb" + file.getName());
            oldThumbnail.delete();

            file.delete();
            tempFile.delete();
        }
        return true;
    }

    public void ecbUpdateFailed(){
        File vaultHeader = new File(path + "/.vault");
        vaultHeader.delete();
    }

    public String getPath() {
        return path;
    }

    private static boolean fileFilter(java.io.File file) {
        String regex = "^((?!_thumb|.thumb|.nomedia|.vault|.header).)*$";   //Filter out .nomedia, .thumb and .header
        String name = file.getName();
        final Pattern p = Pattern.compile(regex);
        p.matcher(name).matches();
        return p.matcher(name).matches();
    }

    public String getName() {
        return name;
    }

    public void iterateAllFiles(onFileFoundListener listener) {
        List<File> files = getFileList();
        for (File file : files) {
            try {
                listener.dothis(EncryptedFileFactory.getInstance().loadEncryptedFile(file,
                        crypter, false));
            } catch (FileNotFoundException e) {
                //Ignore
            }
        }
    }

    public EncryptedFile addFile(final Context context, final Uri uri) throws SecrecyFileException{
        Util.log("Vault: adding file ", uri);
        return EncryptedFileFactory.getInstance().createNewEncryptedFile(
                (new File(FileUtils.getPath(context, uri))), crypter, this);
    }

    public Boolean delete() {
        if (!wrongPass)
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        return !wrongPass;
    }

    public void deleteFile(EncryptedFile file){
        crypter.deleteFile(file);
    }

    private List<File> getFileList() {
        File folder = new File(path);
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
        File folder = new File(path);
        File newFolder = new File(folder.getParent(), name);
        if (folder.getAbsolutePath().equals(newFolder.getAbsolutePath()))
            return this; //same name, bye
        try {
            org.apache.commons.io.FileUtils.moveDirectory(folder, newFolder);
        } catch (IOException e) {
              return null;
        }
        return VaultHolder.getInstance().createAndRetrieveVault(name, passphrase);
    }

    public interface onFileFoundListener {
        void dothis(EncryptedFile encryptedFile);
    }

    public boolean changePassphrase(String oldPassphrase, String newPassphrase){
        return crypter.changePassphrase(oldPassphrase, newPassphrase);
    }
}
