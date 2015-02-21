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
package com.doplgangr.secrecy.filesystem.encryption;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.filesystem.Base64Coder;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@Deprecated
class AES_ECB_Crypter implements Crypter {

    private final String mode = "AES/ECB/PKCS5Padding";
    private final String vaultPath;
    private SecretKeySpec aesKey;

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

        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile), Config.BLOCK_SIZE);

        return new CipherOutputStream(bufferedOutputStream, c);
    }

    @Override
    public SecrecyCipherInputStream getCipherInputStream(File encryptedFile) throws SecrecyCipherStreamException, FileNotFoundException {
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

        return new SecrecyCipherInputStream(new FileInputStream(encryptedFile), c);
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

    @Override
    public boolean changePassphrase(String oldPassphrase, String newPassphrase) {
        return false;
    }

    @Override
    public void deleteFile(EncryptedFile file) {
        file.delete();
    }

    @Override
    public void renameFile(File file, String newName) throws SecrecyCipherStreamException, FileNotFoundException {

        File parent = file.getParentFile();
        newName = FilenameUtils.removeExtension(newName);
        newName = Base64Coder.encodeString(newName);
        newName += "." + FilenameUtils.getExtension(file.getName());
        file.renameTo(new File(parent, newName));
    }
}