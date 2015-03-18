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

package com.doplgangr.secrecy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.doplgangr.secrecy.R;
import com.ipaulpro.afilechooser.FileChooserActivity;

import java.util.ArrayList;


public class ChooseFolder extends ActionBarActivity {
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();

    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, FileChooserActivity.class);

        intent.putStringArrayListExtra(
                FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                INCLUDE_EXTENSIONS_LIST);
        intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

}
