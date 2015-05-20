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
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.fragments.FileChooserFragment;

import java.io.File;


public class FileChooserActivity extends ActionBarActivity implements FileChooserFragment.OnFileChosen{
    public static final String FOLDERS_ONLY = "FOLDERS_ONLY"; // folders only extra
    public static final String FILE_SELECTED = "FILE_SELECTED"; // selected file extra
    public static final String ROOT_FOLDER = "ROOT_FOLDER"; // root folder extra
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar!=null)
            setSupportActionBar(mToolbar);
        Intent intent = getIntent();
        File root = null;
        Boolean foldersOnly = false;
        if (intent!=null) {
            String root_folder = intent.getStringExtra(ROOT_FOLDER);
            if (root_folder!=null)
                root=new File(root_folder);
            foldersOnly = intent.getBooleanExtra(FOLDERS_ONLY, false);
        }
        FileChooserFragment fragment = FileChooserFragment.newInstance(root, foldersOnly);
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void onFileSelected(File path, Boolean confirmed) {
        if (confirmed) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(FILE_SELECTED,path);
            setResult(RESULT_OK, returnIntent);
            finish();
        }else {
            FileChooserFragment fragment = FileChooserFragment.newInstance(path, true);
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(path.getAbsolutePath()).commit();
        }
    }
}
