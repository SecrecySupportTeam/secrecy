package com.doplgangr.secrecy.utils;

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

import android.content.Context;

import com.path.android.jobqueue.network.NetworkEventProvider;
import com.path.android.jobqueue.network.NetworkUtil;

/**
 * Modified implementation for network Utility to NOT observe network events.
 * <p/>
 * Created so that the app does not require the use of network status permission.
 */
public class NetworkUtilImpl implements NetworkUtil, NetworkEventProvider {
    public NetworkUtilImpl(Context context) {
    }

    @Override
    public boolean isConnected(Context context) {
        return false;   //Do not bother the network status since we always do offline work.
    }

    @Override
    public void setListener(Listener listener) {
        //OK. nothing to do.
    }
}