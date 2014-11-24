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

import com.doplgangr.secrecy.Exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;

import java.io.File;
import java.io.FileNotFoundException;

import javax.crypto.CipherOutputStream;

public interface Crypter {

    public CipherOutputStream getCipherOutputStream(File file, String outputFileName) throws SecrecyCipherStreamException, FileNotFoundException;
    public SecrecyCipherInputStream getCipherInputStream(File encryptedFile) throws SecrecyCipherStreamException, FileNotFoundException;
    public String getDecryptedFileName(File file) throws SecrecyCipherStreamException, FileNotFoundException;
    public void deleteFile(EncryptedFile file);
    public boolean changePassphrase(String oldPassphrase, String newPassphrase);
}
