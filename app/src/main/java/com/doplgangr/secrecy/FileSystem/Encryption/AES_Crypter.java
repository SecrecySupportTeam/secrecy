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

package com.doplgangr.secrecy.FileSystem.Encryption;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.Exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.Exceptions.SecrecyFileException;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.FileSystem.Files.SecrecyHeaders.FileHeader;
import com.doplgangr.secrecy.FileSystem.Files.SecrecyHeaders.VaultHeader;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Util;
import com.google.protobuf.ByteString;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

abstract class AES_Crypter implements Crypter {

    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String HEADER_ENCRYPTION_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String VAULT_HEADER_FILENAME = "/.vault";
    private static final String FILE_HEADER_PREFIX = "/.header_";
    private static final int NONCE_LENGTH_BYTE = 16;
    private static final int AES_KEY_SIZE_BIT = 256;
    private static final int SALT_SIZE_BYTE = 16;
    private static final int VAULT_HEADER_VERSION = 1;
    private static final int FILE_HEADER_VERSION = 1;

    private final SecureRandom secureRandom;
    private final String vaultPath;
    private final String encryptionMode;

    private SecretKey vaultFileEncryptionKey;
    private VaultHeader vaultHeader;

    protected AES_Crypter(String vaultPath, String passphrase, String encryptionMode)
            throws InvalidKeyException {
        secureRandom = new SecureRandom();
        this.vaultPath = vaultPath;
        this.encryptionMode = encryptionMode;

        File headerFile = new File(this.vaultPath + VAULT_HEADER_FILENAME);
        if (!headerFile.exists()) {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
                keyGenerator.init(AES_KEY_SIZE_BIT);
                Key encryptionKey = keyGenerator.generateKey();

                byte[] vaultNonce = new byte[NONCE_LENGTH_BYTE];
                byte[] salt = new byte[SALT_SIZE_BYTE];
                secureRandom.nextBytes(vaultNonce);
                secureRandom.nextBytes(salt);

                int pbkdf2Iterations = generatePBKDF2IterationCount(passphrase, salt);

                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
                SecretKey keyFromPassphrase = secretKeyFactory.generateSecret(
                        new PBEKeySpec(passphrase.toCharArray(), salt,
                                pbkdf2Iterations, AES_KEY_SIZE_BIT));

                writeVaultHeader(headerFile, vaultNonce, salt, pbkdf2Iterations, encryptionKey,
                        keyFromPassphrase);
            } catch (Exception e) {
                Util.log("Cannot create vault header!");
                e.printStackTrace();
            }
        }

        try {
            FileInputStream headerInputStream = new FileInputStream(headerFile);
            vaultHeader = VaultHeader.parseFrom(headerInputStream);
        } catch (Exception e) {
            Util.log("Cannot read vault header!");
            e.printStackTrace();
        }

        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
            SecretKey keyFromPassphrase = secretKeyFactory.generateSecret(
                    new PBEKeySpec(passphrase.toCharArray(), vaultHeader.getSalt().toByteArray(),
                            vaultHeader.getPbkdf2Iterations(), AES_KEY_SIZE_BIT));
            Cipher c = Cipher.getInstance(HEADER_ENCRYPTION_MODE);
            c.init(Cipher.UNWRAP_MODE, keyFromPassphrase, new IvParameterSpec(
                    vaultHeader.getVaultIV().toByteArray()));

            vaultFileEncryptionKey = (SecretKey) c.unwrap(vaultHeader.getEncryptedAesKey().toByteArray(),
                    KEY_ALGORITHM, Cipher.SECRET_KEY);
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException("Passphrase is wrong!");
        } catch (Exception e) {
            Util.log("Cannot decrypt AES key");
            e.printStackTrace();
        }
    }

    private static int generatePBKDF2IterationCount(String passphrase, byte[] salt) {
        int calculatedIterations = 0;
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray(),
                    salt, Config.PBKDF2_ITERATIONS_BENCHMARK, AES_KEY_SIZE_BIT);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);

            long startTime = System.currentTimeMillis();
            secretKeyFactory.generateSecret(pbeKeySpec);
            long finishTime = System.currentTimeMillis();

            calculatedIterations = (int) ((Config.PBKDF2_ITERATIONS_BENCHMARK / (double) (finishTime - startTime))
                    * Config.PBKDF2_CREATION_TARGET_MS);
        } catch (Exception e) {
            Util.log("Cannot benchmark PBKDF2!");
        }

        if (calculatedIterations > Config.PBKDF2_ITERATIONS_MIN) {
            Util.log("Using " + calculatedIterations + " PBKDF2 iterations");
            return calculatedIterations;
        }
        Util.log("Using " + Config.PBKDF2_ITERATIONS_MIN + " PBKDF2 iterations");
        return Config.PBKDF2_ITERATIONS_MIN;
    }

    private void writeVaultHeader(File headerFile, byte[] vaultNonce, byte[] salt,
                                  int pbkdf2Iterations, Key aesKey,
                                  SecretKey keyFromPassphrase) throws Exception {
        Cipher c = Cipher.getInstance(HEADER_ENCRYPTION_MODE);
        FileOutputStream headerOutputStream = new FileOutputStream(headerFile);

        c.init(Cipher.WRAP_MODE, keyFromPassphrase, new IvParameterSpec(vaultNonce));
        byte[] encryptedAesKey = c.wrap(aesKey);

        VaultHeader.Builder vaultHeaderBuilder = VaultHeader.newBuilder();
        vaultHeaderBuilder.setVersion(VAULT_HEADER_VERSION);
        vaultHeaderBuilder.setSalt(ByteString.copyFrom(salt));
        vaultHeaderBuilder.setVaultIV(ByteString.copyFrom(vaultNonce));
        vaultHeaderBuilder.setPbkdf2Iterations(pbkdf2Iterations);
        vaultHeaderBuilder.setEncryptedAesKey(ByteString.copyFrom(encryptedAesKey));
        vaultHeaderBuilder.build().writeTo(headerOutputStream);
        headerOutputStream.close();
    }

    @Override
    public CipherOutputStream getCipherOutputStream(File file, String outputFileName)
            throws SecrecyCipherStreamException, FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(encryptionMode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }

        File headerFile = new File(vaultPath + FILE_HEADER_PREFIX + outputFileName);
        File outputFile = new File(vaultPath + "/" + outputFileName);

        byte[] fileEncryptionNonce = new byte[NONCE_LENGTH_BYTE];
        byte[] fileNameNonce = new byte[NONCE_LENGTH_BYTE];
        secureRandom.nextBytes(fileEncryptionNonce);
        secureRandom.nextBytes(fileNameNonce);

        try {
            c.init(Cipher.ENCRYPT_MODE, vaultFileEncryptionKey, new IvParameterSpec(fileNameNonce));
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecrecyCipherStreamException("Invalid algorithm parameter!");
        }

        byte[] encryptedFileName;
        try {
            encryptedFileName = c.doFinal(file.getName().getBytes());
        } catch (IllegalBlockSizeException e) {
            throw new SecrecyCipherStreamException("Illegal block size!");
        } catch (BadPaddingException e) {
            throw new SecrecyCipherStreamException("Bad padding");
        }

        FileHeader.Builder fileHeaderBuilder = FileHeader.newBuilder();
        fileHeaderBuilder.setVersion(FILE_HEADER_VERSION);
        fileHeaderBuilder.setFileIV(ByteString.copyFrom(fileEncryptionNonce));
        fileHeaderBuilder.setFileNameIV(ByteString.copyFrom(fileNameNonce));
        fileHeaderBuilder.setEncryptedFileName(ByteString.copyFrom(encryptedFileName));

        FileOutputStream headerOutputStream = new FileOutputStream(headerFile);
        try {
            fileHeaderBuilder.build().writeTo(headerOutputStream);
            headerOutputStream.close();
        } catch (IOException e) {
            throw new SecrecyCipherStreamException("IO exception while writing file header");
        }


        try {
            c.init(Cipher.ENCRYPT_MODE, vaultFileEncryptionKey, new IvParameterSpec(fileEncryptionNonce));
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecrecyCipherStreamException("Invalid algorithm parameter!");
        }

        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile), Config.BLOCK_SIZE);

        return new CipherOutputStream(bufferedOutputStream, c);
    }

    @Override
    public SecrecyCipherInputStream getCipherInputStream(File encryptedFile)
            throws SecrecyCipherStreamException, FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(encryptionMode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }

        File headerFile = new File(encryptedFile.getParent() +
                FILE_HEADER_PREFIX + encryptedFile.getName());
        if (!headerFile.exists()) {
            throw new FileNotFoundException("Header file not found!");
        }

        FileHeader fileHeader;
        try {
            fileHeader = FileHeader.parseFrom(new FileInputStream(headerFile));
        } catch (IOException e) {
            throw new SecrecyCipherStreamException("Cannot parse file header!");
        }

        try {
            c.init(Cipher.DECRYPT_MODE, vaultFileEncryptionKey,
                    new IvParameterSpec(fileHeader.getFileIV().toByteArray()));
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecrecyCipherStreamException("Invalid algorithm parameter!");
        }

        return new SecrecyCipherInputStream(new FileInputStream(encryptedFile), c);
    }

    public String getDecryptedFileName(File file) throws SecrecyCipherStreamException,
            FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(encryptionMode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }

        File headerFile = new File(file.getParent() + FILE_HEADER_PREFIX + file.getName());
        if (!headerFile.exists()) {
            throw new FileNotFoundException("Header file not found!");
        }

        FileHeader fileHeader;
        try {
            fileHeader = FileHeader.parseFrom(new FileInputStream(headerFile));
        } catch (IOException e) {
            throw new SecrecyCipherStreamException("Cannot parse file header!");
        }

        try {
            c.init(Cipher.DECRYPT_MODE, vaultFileEncryptionKey,
                    new IvParameterSpec(fileHeader.getFileNameIV().toByteArray()));
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecrecyCipherStreamException("Invalid algorithm parameter!");
        }
        byte[] decryptedFileName;
        try {
            decryptedFileName = c.doFinal(fileHeader.getEncryptedFileName().toByteArray());
        } catch (IllegalBlockSizeException e) {
            throw new SecrecyCipherStreamException("Illegal block size!");
        } catch (BadPaddingException e) {
            throw new SecrecyCipherStreamException("Bad padding");
        }
        return new String(decryptedFileName);
    }

    @Override
    public void renameFile(File file, String newName) throws SecrecyCipherStreamException, FileNotFoundException {
        Cipher c;
        try {
            c = Cipher.getInstance(encryptionMode);
        } catch (NoSuchAlgorithmException e) {
            throw new SecrecyCipherStreamException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException e) {
            throw new SecrecyCipherStreamException("Selected padding not found!");
        }

        File headerFile = new File(file.getParent() + FILE_HEADER_PREFIX + file.getName());
        if (!headerFile.exists()) {
            throw new FileNotFoundException("Header file not found!");
        }

        FileHeader fileHeader;
        try {
            fileHeader = FileHeader.parseFrom(new FileInputStream(headerFile));
        } catch (IOException e) {
            throw new SecrecyCipherStreamException("Cannot parse file header!");
        }

        try {
            c.init(Cipher.ENCRYPT_MODE, vaultFileEncryptionKey,
                    new IvParameterSpec(fileHeader.getFileNameIV().toByteArray()));
        } catch (InvalidKeyException e) {
            throw new SecrecyCipherStreamException("Invalid encryption key!");
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecrecyCipherStreamException("Invalid algorithm parameter!");
        }

        byte[] encryptedFileName;
        try {
            encryptedFileName = c.doFinal(newName.getBytes());
        } catch (IllegalBlockSizeException e) {
            throw new SecrecyCipherStreamException("Illegal block size!");
        } catch (BadPaddingException e) {
            throw new SecrecyCipherStreamException("Bad padding");
        }

        FileHeader.Builder fileHeaderBuilder = fileHeader.toBuilder();
        fileHeaderBuilder.setEncryptedFileName(ByteString.copyFrom(encryptedFileName));

        FileOutputStream headerOutputStream = new FileOutputStream(headerFile);
        try {
            fileHeaderBuilder.build().writeTo(headerOutputStream);
            headerOutputStream.close();
        } catch (IOException e) {
            throw new SecrecyCipherStreamException("IO exception while writing file header");
        }
    }

    @Override
    public boolean changePassphrase(String oldPassphrase, String newPassphrase) {
        SecretKeyFactory secretKeyFactory;

        File headerFileOld = new File(this.vaultPath + VAULT_HEADER_FILENAME);
        File headerFileNew = new File(this.vaultPath + VAULT_HEADER_FILENAME + "NEW");
        if (!headerFileNew.exists()) {
            try {
                // Decrypt AES encryption key
                secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
                SecretKey oldKeyFromPassphrase = secretKeyFactory.generateSecret(
                        new PBEKeySpec(oldPassphrase.toCharArray(), vaultHeader.getSalt().toByteArray(),
                                vaultHeader.getPbkdf2Iterations(), AES_KEY_SIZE_BIT));
                Cipher c = Cipher.getInstance(HEADER_ENCRYPTION_MODE);
                c.init(Cipher.UNWRAP_MODE, oldKeyFromPassphrase, new IvParameterSpec(
                        vaultHeader.getVaultIV().toByteArray()));
                Key decryptedKey = c.unwrap(vaultHeader.getEncryptedAesKey().toByteArray(),
                        KEY_ALGORITHM, Cipher.SECRET_KEY);

                // Create new vault nonce and salt
                byte[] vaultNonce = new byte[NONCE_LENGTH_BYTE];
                byte[] salt = new byte[SALT_SIZE_BYTE];
                secureRandom.nextBytes(vaultNonce);
                secureRandom.nextBytes(salt);

                int pbkdf2Iterations = generatePBKDF2IterationCount(newPassphrase, salt);

                // Create new key for AES key encryption
                SecretKey newKeyFromPassphrase = secretKeyFactory.generateSecret(
                        new PBEKeySpec(newPassphrase.toCharArray(), salt,
                                pbkdf2Iterations, AES_KEY_SIZE_BIT));

                writeVaultHeader(headerFileNew, vaultNonce, salt, pbkdf2Iterations,
                        decryptedKey, newKeyFromPassphrase);

            } catch (Exception e) {
                Util.log("Error while reading or creating new vault header!");
                return false;
            }
        } else {
            Util.log("New header file already exists. Cannot change passphrase!");
            return false;
        }

        // Try to parse new header file
        try {
            FileInputStream headerInputStream = new FileInputStream(headerFileNew);
            vaultHeader = VaultHeader.parseFrom(headerInputStream);
        } catch (Exception e) {
            Util.log("Cannot read vault header!");
            headerFileNew.delete();
            return false;
        }

        // Delete old header file and replace with new header file
        if (!headerFileOld.delete()) {
            headerFileNew.delete();
            Util.log("Cannot delete old vault header!");
            return false;
        }
        try {
            org.apache.commons.io.FileUtils.copyFile(headerFileNew, headerFileOld);
        } catch (IOException e) {
            Util.log("Cannot replace old vault header!");
            return false;
        }

        headerFileNew.delete();
        return true;
    }

    @Override
    public void deleteFile(EncryptedFile file) {
        Storage.purgeFile(new File(file.getFile().getParent() +
                FILE_HEADER_PREFIX + file.getFile().getName()));
        try {
            Storage.purgeFile(new File(file.getEncryptedThumbnail().getFile().getParent()
                    + FILE_HEADER_PREFIX + file.getEncryptedThumbnail().getFile().getName()));
        } catch (SecrecyFileException e) {
            Util.log("Thumbnail header file not found!");
        }
        file.delete();
    }
}
