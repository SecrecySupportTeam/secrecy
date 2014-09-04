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

package com.doplgangr.secrecy.Views;

import android.view.View;
import android.widget.ProgressBar;

import com.doplgangr.secrecy.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.activity_list_vault)
public class FileImportFragment extends VaultsListFragment {
    @ViewById(R.id.progressBar)
    ProgressBar addFilepBar;

    @AfterViews
    void afterViews() {
        super.oncreate();
        context.setTitle(R.string.Dialog_header__import_files);
        getActivity().supportInvalidateOptionsMenu();
    }


    @Override
    public void setClickListener(final View mView, final int i) {
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                open(adapter.getItem(i), mView, i);

                addFilepBar.setVisibility(View.VISIBLE);
            }
        });
    }


}
