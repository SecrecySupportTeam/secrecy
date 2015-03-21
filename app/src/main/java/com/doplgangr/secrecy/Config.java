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

import com.doplgangr.secrecy.filesystem.files.EncryptedFile;

import java.util.Comparator;

public class Config {
    public static final int BLOCK_SIZE = 4096;
    public static final int BUFFER_SIZE = BLOCK_SIZE * 10;
    public static final int PBKDF2_CREATION_TARGET_MS = 1000;
    public static final int PBKDF2_ITERATIONS_MIN = 4096;
    public static final int PBKDF2_ITERATIONS_BENCHMARK = 20000;
    public static final int IMAGE_SIZE_SMALL = 4 * 1000 * 1000; // Resulting image size: 2 > X > 4
    public static final int IMAGE_SIZE_MEDIUM = 6 * 1000 * 1000; // Resulting image size: 3 > X > 6
    public static final int IMAGE_SIZE_LARGE = 10 * 1000 * 1000; // Resulting image size: 5 > X > 10
    public static int selectedImageSize = 0;
    public static final String file_extra = "FILE";
    public static final String vault_extra = "VAULT";
    public static final String password_extra = "PASS";
    public static final String gallery_item_extra = "GALLERYITEMIS";
    public static final String FIRST_TIME_EXTRA = "FIRSTTIME";
    public static final String tag = "Secrecy";
    public static final String cancellable_task = "CANCELLABLETASK";
    public static final int wrong_password = 1;
    public static final int file_not_found = 2;
    public static final String settingsStore = "__SETTINGS__";
    public static final String root = "__ROOT__";
    public static final String support_website = "http://secrecy.uservoice.com";

    // PreferenceManager strings
    public static final String STEALTH_MODE_PASSWORD = "stealth_mode_password";
    public static final String STEALTH_MODE = "stealth_mode";
    public static final String SHOW_STEALTH_MODE_TUTORIAL =  "showStealthModeTutorial";
    public static final String IMAGE_SIZE = "image_size";
    public static final String IMAGE_SIZE_DEFAULT = "0";
    public static final String VAULT_ROOT = "vault_root";
    public static final String VAULT_SORT = "vault_sort";
    public static final String VAULT_SORT_ALPHABETIC = "ALPHABETIC";
    public static final String VAULT_SORT_FILETYPE = "FILETYPE";
    public static final String VAULT_SORT_LASTMODIFIED = "LASTMODIFIED";
    public static final String APP_VERSION_NUMBER = "appVersionNumber";
    public static final String APP_VERSION_NAME = "appVersionName";

    public static final Comparator<EncryptedFile> COMPARATOR_ENCRYPTEDFILE_ALPHABETIC = new Comparator<EncryptedFile>() {
        @Override
        public int compare(EncryptedFile encryptedFile, EncryptedFile encryptedFile2) {
            return encryptedFile.getDecryptedFileName().compareToIgnoreCase(encryptedFile2.getDecryptedFileName());
        }
    };

    public static final Comparator<EncryptedFile> COMPARATOR_ENCRYPTEDFILE_FILETYPE = new Comparator<EncryptedFile>() {
        // Orders same files with the same type in alphabetic order
        @Override
        public int compare(EncryptedFile encryptedFile, EncryptedFile encryptedFile2) {
            int compare = encryptedFile.getFileExtension().compareToIgnoreCase(encryptedFile2.getFileExtension());
            if (compare == 0) {
                return encryptedFile.getDecryptedFileName().compareToIgnoreCase(encryptedFile2.getDecryptedFileName());
            }
            return compare;
        }
    };

    public static final Comparator<EncryptedFile> COMPARATOR_ENCRYPTEDFILE_LASTMODIFIED= new Comparator<EncryptedFile>() {
        // Reverse order - Last modified.
        // Rarely two files have identical timestamp. OK to have to alternative sorting method.
        @Override
        public int compare(EncryptedFile encryptedFile, EncryptedFile encryptedFile2) {
            return encryptedFile2.getDate().compareTo(encryptedFile.getDate());
        }
    };
}
