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

package com.doplgangr.secrecy.filesystem.encryption;

import com.doplgangr.secrecy.utils.Util;

public class VaultHolder {

    private final static VaultHolder INSTANCE = new VaultHolder();
    private Vault vault;

    public static VaultHolder getInstance() {
        return VaultHolder.INSTANCE;
    }

    public Vault createAndRetrieveVault(String name, String password) {
        Vault newVault = new Vault(name, password);
        if (newVault.wrongPass) {
            return newVault;
        }
        clear();
        vault = newVault;
        return vault;
    }

    public Vault retrieveVault() {
        return vault;
    }

    public void clear() {
        Util.log("VaultHolder cleared!");
        vault = null;
    }
}