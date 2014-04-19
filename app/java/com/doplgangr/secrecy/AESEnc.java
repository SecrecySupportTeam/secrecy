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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESEnc {

    private final String KEY_ALGORITHM = "AES";
    private String mode = "AES/ECB/PKCS5Padding";
    private SecretKeySpec aesKey;
    private byte[] key;

    public AESEnc(String text) {
        try {
            byte[] bytes = text.getBytes("UTF-8");
            byte[] legitkey = new byte[32];
            int i = 0;
            while (i < 32 && i < bytes.length) {
                legitkey[i] = bytes[i];
                i++;
            }
            aesKey = new SecretKeySpec(legitkey, KEY_ALGORITHM);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private byte[] generateAesKey() {


        KeyGenerator kgen = null;
        try {
            kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        kgen.init(256);
        SecretKey secretKey = kgen.generateKey();

        return secretKey.getEncoded();
    }

    public Cipher encryptstream() throws Exception {

        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.ENCRYPT_MODE, aesKey);
            return c;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public Cipher decryptstream() throws Exception {

        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.DECRYPT_MODE, aesKey);

            return c;
        } catch (Exception ex) {
            throw ex;
        }
    }


    public byte[] getKey() {
        return key;
    }

}