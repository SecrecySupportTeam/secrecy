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

package com.doplgangr.secrecy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.doplgangr.secrecy.R;

import java.util.ArrayList;
import java.util.Collections;

public class VaultsAdapter extends BaseAdapter {
    // store the context (as an inflated layout)
    private final LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private final int resource;
    // store (a reference to) the data
    private ArrayList<String> data = new ArrayList<String>();

    public VaultsAdapter(Context context, ArrayList<String> data) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = R.layout.vault_item;
        if (data != null)
            this.data = data;
    }

    public void add(String item) {
        data.add(item);
    }


    /**
     * Return the size of the data set.
     */
    public int getCount() {
        return this.data.size();
    }

    /**
     * Return an object in the data set.
     */
    public String getItem(int position) {
        return this.data.get(position);
    }

    /**
     * Return the position provided.
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Return a generated view for a position.
     */
    public void update(ArrayList<String> data) {
        this.data = data;
    }

    public void sort(){
        Collections.sort(data);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // reuse a given view, or inflate a new one from the xml
        View view;

        if (convertView == null) {
            view = this.inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        // bind the data to the view object
        return this.bindData(view, position);
    }

    public View getView(int position, ViewGroup parent) {
        // reuse a given view, or inflate a new one from the xml
        View view;

        view = this.inflater.inflate(resource, parent, false);

        // bind the data to the view object
        return this.bindData(view, position);
    }

    /**
     * Bind the provided data to the view.
     * This is the only method not required by base adapter.
     */
    View bindData(View view, final int position) {
        // pull out the object
        if (this.data.size() <= position)
            return view; //whattt???? Abort! abort!
        String vault = this.data.get(position);

        // extract the view object
        View viewElement = view.findViewById(R.id.name);
        // cast to the correct type
        TextView tv = (TextView) viewElement;
        // set the value
        tv.setText(vault);
        // return the final view object

        return view;
    }

}
