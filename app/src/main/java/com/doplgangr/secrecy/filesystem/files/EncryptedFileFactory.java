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

package com.doplgangr.secrecy.filesystem.files;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.exceptions.SecrecyFileException;
import com.doplgangr.secrecy.filesystem.encryption.Crypter;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import javax.crypto.CipherOutputStream;

public class EncryptedFileFactory {

    private final static EncryptedFileFactory INSTANCE = new EncryptedFileFactory();
    private final static String THUMBNAIL_PREFIX = "/.thumb_";
    private static final String FILE_HEADER_PREFIX = "/.header_";

    public static EncryptedFileFactory getInstance() {
        return EncryptedFileFactory.INSTANCE;
    }

    public EncryptedFile loadEncryptedFile(File encryptedFile, Crypter crypter, boolean isEcbVault)
            throws FileNotFoundException {

        File thumbnail = new File(encryptedFile.getParent() +
                THUMBNAIL_PREFIX + encryptedFile.getName());
        File fileHeader = new File(encryptedFile.getParent() +
                FILE_HEADER_PREFIX + encryptedFile.getName());
        if (!isEcbVault && !fileHeader.exists()){
            throw new FileNotFoundException("File header not found!");
        }

        if (thumbnail.exists()) {
            EncryptedThumbnail encryptedThumbnail = new EncryptedThumbnail(
                    thumbnail, crypter);
            return new EncryptedFile(encryptedFile, crypter, encryptedThumbnail);
        }
        return new EncryptedFile(encryptedFile, crypter, null);
    }

    public EncryptedFile createNewEncryptedFile(File unencryptedFile, Crypter crypter, Vault vault)
            throws SecrecyFileException {
        Util.log(this.getClass().getName() + ": Creating new encrypted file!");

        String outputFileName = UUID.randomUUID().toString();
        File outputFile = new File(vault.getPath() + "/" + outputFileName);

        while (outputFile.exists()) {
            Util.log("Output file name already exists. Trying new file name!");
            outputFileName = UUID.randomUUID().toString();
            outputFile = new File(vault.getPath() + "/" + outputFileName);
        }

        BufferedInputStream bufferedInputStream;
        CipherOutputStream out;

        try {
            bufferedInputStream = new BufferedInputStream(
                    new FileInputStream(unencryptedFile), Config.BLOCK_SIZE);
            out = crypter.getCipherOutputStream(unencryptedFile, outputFileName);
        } catch (FileNotFoundException e) {
            Util.log(this.getClass().getName() + ": File not found!");
            throw new SecrecyFileException("File not found!");
        } catch (SecrecyCipherStreamException e) {
            Util.log(this.getClass().getName() + ": SecrecyCipherStreamException: " + e.getMessage());
            throw new SecrecyFileException("SecrecyCipherStreamException: " + e.getMessage());
        }

        try {
            if (out != null) {
                byte buffer[] = new byte[Config.BUFFER_SIZE];
                int count;
                while ((count = bufferedInputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            }
        } catch (IOException e) {
            Util.log(this.getClass().getName() + ": IOException while encrypting file!");
            throw new SecrecyFileException("IOException while encrypting file!");
        } finally {
            try {
                bufferedInputStream.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Util.log(this.getClass().getName() + ": IOException while closing IO streams");
            }
        }

        // Create thumbnail
        File thumbnailOutputFile = new File(vault.getPath() + THUMBNAIL_PREFIX + outputFileName);
        createThumbnail(unencryptedFile, thumbnailOutputFile, crypter);
        EncryptedThumbnail encryptedThumbnail = new EncryptedThumbnail(thumbnailOutputFile, crypter);


        EncryptedFile encryptedFile;
        try {
            encryptedFile = new EncryptedFile(outputFile, crypter, encryptedThumbnail);
        } catch (FileNotFoundException e) {
            Util.log(this.getClass().getName() + ": Encrypted file not found!");
            throw new SecrecyFileException("Encrypted file not found!");
        }
        return encryptedFile;
    }

    private boolean createThumbnail(File inputFile, File outputFile, Crypter crypter) {
        Bitmap bitmap;
        Util.log("Trying to create thumbnail for: " + inputFile.getPath());
        bitmap = Storage.decodeSampledBitmapFromPath(inputFile.getPath(),
                150, 150);
        if (bitmap == null) {   //If photo fails, try Video
            Util.log("Trying to create video thumbnail for: " + inputFile.getPath());
            bitmap = ThumbnailUtils.createVideoThumbnail(inputFile.getPath(),
                    MediaStore.Video.Thumbnails.MICRO_KIND);
        }
        if (bitmap == null) {
            Util.log("Could not create thumbnail for: " + inputFile.getPath());
            return false;
        }

        CipherOutputStream out;
        try {
            out = crypter.getCipherOutputStream(outputFile, outputFile.getName());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (SecrecyCipherStreamException e) {
            return false;
        } catch (FileNotFoundException e) {
            return false;
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}


