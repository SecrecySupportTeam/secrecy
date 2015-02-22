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

import com.doplgangr.secrecy.filesystem.encryption.Crypter;
import com.doplgangr.secrecy.filesystem.encryption.SecrecyCipherInputStream;
import com.doplgangr.secrecy.filesystem.Storage;

import java.io.File;

public class EncryptedThumbnail extends SecrecyFile {

    private Bitmap thumbBitmap = null;

    EncryptedThumbnail(File file, Crypter crypter) {
        this.crypter = crypter;
        this.file = file;
    }

    public Bitmap getThumb(int thumbnailSize) {
        if ((thumbBitmap == null)) {
            SecrecyCipherInputStream streamThumb = readStream();
            this.thumbBitmap = Storage.getThumbnailfromStream(streamThumb, thumbnailSize);
        }
        return thumbBitmap;
    }

    @Override
    public void delete() {
        Storage.purgeFile(file);
    }
}