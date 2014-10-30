/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.doplgangr.secrecy.FileSystem.Encryption;

import android.content.ContentResolver;
import android.webkit.MimeTypeMap;

import com.doplgangr.secrecy.Exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.FileSystem.Base64Coder;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@Deprecated
class AES_ECB_Crypter implements Crypter {

    private final String mode = "AES/ECB/PKCS5Padding";
    private SecretKeySpec aesKey;
    private String vaultPath;
    private byte[] key;

    public AES_ECB_Crypter(String vaultPath, String password) throws InvalidKeyException{
        this.vaultPath = vaultPath;
        try {
            byte[] bytes = password.getBytes("UTF-8");
            byte[] legitkey = new byte[32];
            int i = 0;
            while (i < 32 && i < bytes.length) {
                legitkey[i] = bytes[i];
                i++;
            }
            String KEY_ALGORITHM = "AES";
            aesKey = new SecretKeySpec(legitkey, KEY_ALGORITHM);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CipherOutputStream getCipherOutputStream(File file, String outputFileName)
            throws SecrecyCipherStreamException, FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(mode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }


        try {
            c.init(Cipher.ENCRYPT_MODE, aesKey);
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        }

        String filename = Base64Coder.encodeString(FilenameUtils.removeExtension(file.getName()))
                + "." + FilenameUtils.getExtension(file.getName());
        File outputFile = new File(vaultPath + "/" + filename);

        return new CipherOutputStream(new FileOutputStream(outputFile), c);
    }

    @Override
    public CipherInputStream getCipherInputStream(File encryptedFile) throws SecrecyCipherStreamException, FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(mode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }

        try {
            c.init(Cipher.DECRYPT_MODE, aesKey);
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        }

        return new CipherInputStream(new FileInputStream(encryptedFile), c);
    }

    @Override
    public String getDecryptedFileName(File file)
            throws SecrecyCipherStreamException, FileNotFoundException {

        String name = FilenameUtils.getBaseName(file.getName());
        try {
            name = Base64Coder.decodeString(name); //if name is invalid, return original name
        } catch (IllegalArgumentException ignored) {
        }
        name += "." + FilenameUtils.getExtension(file.getName());
        return name;
    }

    public byte[] getKey() {
        return key;
    }
}