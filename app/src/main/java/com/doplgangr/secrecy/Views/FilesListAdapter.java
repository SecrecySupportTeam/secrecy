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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.R;

import java.util.ArrayList;

class FilesListAdapter extends BaseAdapter {
    // store the context (as an inflated layout)
    private final LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private final int resource;
    private final ArrayList<Integer> checked = new ArrayList<Integer>();
    // store (a reference to) the data
    private ArrayList<File> data = new ArrayList<File>();

    public FilesListAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = R.layout.file_item;
    }

    /**
     * Add data to data set.
     */
    public void add(File file) {
        data.add(file);
        notifyDataSetChanged();
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
    public File getItem(int position) {
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
    public void update(ArrayList<File> data) {
        this.data = data;
        checked.clear();
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

    /**
     * Bind the provided data to the view.
     * This is the only method not required by base adapter.
     */
    View bindData(final View view, int position) {
        // make sure it's worth drawing the view
        if (position >= this.data.size())  // To prevent out of bound exception
            return view;
        if (this.data.get(position) == null)
            return view;

        // pull out the object
        File file = this.data.get(position);

        // extract the view object
        View viewElement = view.findViewById(R.id.name);
        // cast to the correct type
        TextView tv = (TextView) viewElement;
        // set the value
        tv.setText(file.getName());

        viewElement = view.findViewById(R.id.type);
        tv = (TextView) viewElement;
        tv.setText(file.getType());

        viewElement = view.findViewById(R.id.size);
        tv = (TextView) viewElement;
        tv.setText(file.getSize());

        viewElement = view.findViewById(R.id.date);
        tv = (TextView) viewElement;
        tv.setText(file.getTimestamp());

        // This class is for binding thumbnail to UI
        class BindImageTask extends AsyncTask<File, Void, Bitmap> {
            protected Bitmap doInBackground(File... files) {
                return files[0].getThumb();     // async decrypt thumbnail
            }

            protected void onPostExecute(Bitmap thumbnail) {
                View viewElement = view.findViewById(R.id.thumbNail);
                ImageView iv = (ImageView) viewElement;
                if (thumbnail != null)
                    iv.setImageBitmap(thumbnail);   // bind thumbnail in UI thread
            }
        }
        new BindImageTask().execute(file);

        // return the final view object
        return view;
    }

    public Boolean select(int position) {
        if (checked.contains(position))
            checked.remove(checked.indexOf(position));
        else
            checked.add(position);
        return checked.contains(position);
    }

    public ArrayList<Integer> getSelected() {
        return checked;
    }

    public void clearSelected() {
        checked.clear();
    }

    public void clear() {
        data.clear();
    }


}
