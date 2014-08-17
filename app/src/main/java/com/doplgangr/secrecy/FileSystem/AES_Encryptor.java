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

package com.doplgangr.secrecy.FileSystem;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class AES_Encryptor {

    private final String mode = "AES/ECB/PKCS5Padding";
    private SecretKeySpec aesKey;
    private byte[] key;

    public AES_Encryptor(String text) {
        try {
            byte[] bytes = text.getBytes("UTF-8");
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

    public Cipher encryptstream() throws Exception {

        Cipher c = Cipher.getInstance(mode);
        c.init(Cipher.ENCRYPT_MODE, aesKey);
        return c;
    }

    public Cipher decryptstream() throws Exception {

        Cipher c = Cipher.getInstance(mode);
        c.init(Cipher.DECRYPT_MODE, aesKey);
        return c;
    }


    public byte[] getKey() {
        return key;
    }

}