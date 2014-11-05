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

public class Config {
    public static final int BLOCK_SIZE = 4096;
    public static final int BUFFER_SIZE = BLOCK_SIZE * 10;
    public static final int PBKDF2_CREATION_TARGET_MS = 1000;
    public static final int PBKDF2_ITERATIONS_MIN = 4096;
    public static final int PBKDF2_ITERATIONS_BENCHMARK = 20000;
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

}
