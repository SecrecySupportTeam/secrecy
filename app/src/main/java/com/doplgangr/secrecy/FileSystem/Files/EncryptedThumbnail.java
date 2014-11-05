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

import android.graphics.Bitmap;

import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Encryption.Crypter;
import com.doplgangr.secrecy.FileSystem.Encryption.SecrecyCipherInputStream;
import com.doplgangr.secrecy.FileSystem.Storage;

import java.io.File;

public class EncryptedThumbnail extends SecrecyFile {

    private Bitmap thumbBitmap = null;
    private final boolean thumbnailCreated = false;

    EncryptedThumbnail(File file, Crypter crypter) {
        this.crypter = crypter;
        this.file = file;
    }

    public boolean isThumbnailCreated() {
        return thumbnailCreated;
    }

    public Bitmap getThumb(int thumbnailSize) {
        if ((!thumbnailCreated) && (thumbBitmap == null)) {
            SecrecyCipherInputStream streamThumb = readStream(new CryptStateListener() {
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
            this.thumbBitmap = Storage.getThumbnailfromStream(streamThumb, thumbnailSize);
        }
        return thumbBitmap;
    }
}