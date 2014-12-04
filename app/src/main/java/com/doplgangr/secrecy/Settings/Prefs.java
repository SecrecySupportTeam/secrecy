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

package com.doplgangr.secrecy.Settings;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.UNIQUE)
public interface Prefs {
    String OpenPIN();

    int stealthMode();

    int maxImageSize();

    @DefaultBoolean(true)
    boolean analytics();

    @DefaultBoolean(false)
    boolean stealth();

    @DefaultBoolean(false)
    boolean sorting();

    @DefaultBoolean(true)
    boolean showVaultSwipeDeleteTutorial();

    @DefaultBoolean(true)
    boolean showVaultLongPressRenameTutorial();

    @DefaultBoolean(true)
    boolean showHelpDeskTutorial();
}
