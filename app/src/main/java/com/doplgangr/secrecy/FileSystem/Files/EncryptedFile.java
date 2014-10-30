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

package com.doplgangr.secrecy.FileSystem.Files;

/**
 * This is an alias of a FILE in the secrecy system.
 *
 */

import com.doplgangr.secrecy.Exceptions.SecrecyFileException;
import com.doplgangr.secrecy.FileSystem.Encryption.Crypter;
import com.doplgangr.secrecy.FileSystem.Storage;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

public class EncryptedFile extends SecrecyFile {

    private EncryptedThumbnail encryptedThumbnail;
    private Boolean hasThumbnail = false;

    /**
     * Loads an existing encrypted file from the file system.
     *
     * @param file
     * @param crypter
     */
    EncryptedFile(File file, Crypter crypter, EncryptedThumbnail encryptedThumbnail)
            throws FileNotFoundException {
        this.file = file;
        this.crypter = crypter;
        this.encryptedThumbnail = encryptedThumbnail;

        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        try {
            decryptedFileName = crypter.getDecryptedFileName(file);
        } catch (Exception e) {
            //Ignored
        }

        if (encryptedThumbnail != null && encryptedThumbnail.isThumbnailCreated()) {
            hasThumbnail = true;
        }

        fileExtension = FilenameUtils.getExtension(decryptedFileName);
        this.fileSize = humanReadableByteCount(file.length());
        timestamp = new Date(file.lastModified());
    }

    public boolean hasThumbnail() {
        return (hasThumbnail);
    }

    public EncryptedThumbnail getEncryptedThumbnail() throws SecrecyFileException {
        if (encryptedThumbnail == null) {
            throw new SecrecyFileException("No encrypted thumbnail available!");
        }
        return encryptedThumbnail;
    }

    @Override
    public void delete() {
        Storage.purgeFile(encryptedThumbnail.getFile());
        Storage.purgeFile(file);
    }
}
