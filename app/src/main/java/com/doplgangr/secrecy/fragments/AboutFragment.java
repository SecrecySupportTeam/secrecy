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

package com.doplgangr.secrecy.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;

public class AboutFragment extends PreferenceFragment {
    private Context context;

    private String[] creditsNames;
    private String[] creditsDescription;
    private String[] creditsLinks;
    private String[] contributorNames;
    private String[] contributorDescription;
    private String[] contributorLinks;
    private String libraries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_about);
        this.context = getActivity();

        Resources res = getResources();
        creditsNames = res.getStringArray(R.array.Credits__names);
        creditsDescription = res.getStringArray(R.array.Credits__description);
        creditsLinks = res.getStringArray(R.array.Credits__links);
        contributorNames = res.getStringArray(R.array.Contributor__names);
        contributorDescription = res.getStringArray(R.array.Contributor__description);
        contributorLinks = res.getStringArray(R.array.Contributor__links);
        libraries = getString(R.string.Settings__libraries_message);

        prepareCreditList();
        prepareTranslatorsList();
        prepareVersion();
        prepareLegal();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((ActionBarActivity) getActivity()).getSupportActionBar()
                .setTitle(R.string.Page_header__about);

        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    private void prepareCreditList() {
        PreferenceGroup creditsList = (PreferenceGroup) findPreference("credits_list");
        for (int i = 0; i < creditsNames.length; i++) {
            Preference newPreference = new Preference(context);
            newPreference.setTitle(creditsNames[i]);
            newPreference.setSummary(creditsDescription[i]);
            final int finali = i;
            newPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Uri uri = Uri.parse(creditsLinks[finali]);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
            creditsList.addPreference(newPreference);
        }
    }

    private void prepareTranslatorsList() {
        PreferenceGroup translatorList = (PreferenceGroup) findPreference("translators_list");
        for (int i = 0; i < contributorNames.length; i++) {
            Preference newPreference = new Preference(context);
            newPreference.setTitle(contributorNames[i]);
            newPreference.setSummary(contributorDescription[i]);
            final int finali = i;
            newPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Uri uri = Uri.parse(contributorLinks[finali]);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
            translatorList.addPreference(newPreference);
        }
    }

    private void prepareVersion() {
        Preference version = findPreference("version");
        version.setSummary(CustomApp.VERSIONNAME);
    }

    private void prepareLegal() {
        Preference dialogPreference = getPreferenceScreen().findPreference("legal");
        dialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Util.alert(context,
                        null,
                        libraries,
                        Util.emptyClickListener,
                        null);
                return true;
            }
        });
    }
}